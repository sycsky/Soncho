import os
import json
import subprocess
import sys

def run_command(command):
    try:
        print(f"Running: {command}")
        result = subprocess.run(
            command,
            check=True,
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        print(result.stdout)
        return True
    except subprocess.CalledProcessError as e:
        print(f"Error running command: {command}")
        print(e.stderr)
        return False

def get_current_branch():
    # In GitHub Actions, GITHUB_REF is like 'refs/heads/branch_name'
    github_ref = os.environ.get('GITHUB_REF')
    if github_ref and github_ref.startswith('refs/heads/'):
        return github_ref[11:]
    
    # Fallback for local testing
    try:
        result = subprocess.run(
            "git rev-parse --abbrev-ref HEAD",
            shell=True,
            check=True,
            stdout=subprocess.PIPE,
            text=True
        )
        return result.stdout.strip()
    except:
        return None

def load_config(config_path):
    with open(config_path, 'r') as f:
        return json.load(f)

def main():
    config_path = 'branch-config.json'
    if not os.path.exists(config_path):
        print(f"Config file {config_path} not found.")
        sys.exit(0) # Not an error, just nothing to do

    config = load_config(config_path)
    mapping = config.get('branches', {})
    
    current_branch = get_current_branch()
    print(f"Current branch: {current_branch}")
    
    if not current_branch:
        print("Could not determine current branch.")
        sys.exit(1)

    # Configure git user for commits
    run_command('git config --global user.email "github-actions[bot]@users.noreply.github.com"')
    run_command('git config --global user.name "github-actions[bot]"')

    # Ensure we are on the correct branch (not detached HEAD) if possible
    # But usually GHA checks out sha. We want the branch tip.
    # Actually we should fetch everything first.
    # run_command("git fetch --all") # This is done in workflow with fetch-depth: 0 usually, but let's be safe if needed.
    
    # BFS queue
    queue = [current_branch]
    visited = set()
    
    merge_errors = []

    while queue:
        parent = queue.pop(0)
        
        if parent in visited:
            continue
        visited.add(parent)
        
        children = mapping.get(parent, [])
        if not children:
            continue

        print(f"Processing children of {parent}: {children}")

        for child in children:
            print(f"Attempting to merge {parent} into {child}...")
            
            # Checkout child branch
            # We need to make sure we have the latest child from remote
            # Strategy: 
            # 1. Fetch origin
            # 2. Checkout child (track origin/child if local doesn't exist)
            # 3. Pull origin child to be sure we are up to date
            # 4. Merge parent (local parent, which we might have just updated in previous loop)
            # 5. Push child
            
            # Note: parent is already locally updated because:
            # - If parent is current_branch, we are on it (or we should be).
            # - If parent was a child in previous iteration, we checked it out, merged, and pushed it. So local parent is up to date.
            
            # Fetch origin to ensure we know about child branch
            run_command(f"git fetch origin {child}")

            # Checkout
            if run_command(f"git show-ref --verify --quiet refs/heads/{child}"):
                # Local branch exists
                run_command(f"git checkout {child}")
                # Update local child with remote child
                run_command(f"git pull origin {child}")
            else:
                # Local branch does not exist, try checkout from origin
                if not run_command(f"git checkout -b {child} origin/{child}"):
                    print(f"Branch {child} does not exist on remote. Skipping.")
                    continue
            
            # Merge parent
            # parent should be a valid local ref now
            if not run_command(f"git merge {parent}"):
                error_msg = f"Merge conflict or error merging {parent} into {child}."
                print(f"{error_msg} Aborting merge and skipping this path.")
                merge_errors.append(error_msg)
                run_command("git merge --abort")
                continue
            
            # Push changes
            if not run_command(f"git push origin {child}"):
                print(f"Failed to push {child}. Skipping cascade for this path.")
                continue
            
            print(f"Successfully merged {parent} into {child} and pushed.")
            queue.append(child)
            
    if merge_errors:
        print("\n=== Merge Errors Summary ===")
        with open("merge_report.txt", "w") as f:
            f.write("The following merge conflicts or errors occurred:\n\n")
            for error in merge_errors:
                print(error)
                f.write(f"- {error}\n")
        
        sys.exit(1)

if __name__ == "__main__":
    main()
