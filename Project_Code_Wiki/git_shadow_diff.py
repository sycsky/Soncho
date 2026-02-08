import argparse
import os
import subprocess
import sys


def print_run_hint():
    print("请在 git 仓库根目录执行该脚本，例如：")
    print("  cd D:/ai_agent_work/ai_agent_workflow/ai_kef")
    print("  python Project_Code_Wiki/git_shadow_diff.py")


def run_git(args):
    result = subprocess.run(["git"] + args, capture_output=True, text=True)
    if result.returncode != 0:
        error_message = result.stderr.strip()
        if error_message:
            print(error_message)
        print_run_hint()
        sys.exit(result.returncode)
    return result.stdout


def get_repo_root():
    return run_git(["rev-parse", "--show-toplevel"]).strip()


def normalize_path(path_value):
    return path_value.replace("\\", "/")


def get_staged_files():
    output = run_git(["diff", "--cached", "--name-only"])
    return [line.strip() for line in output.splitlines() if line.strip()]


def get_working_tree_files():
    output = run_git(["diff", "--name-only"])
    return [line.strip() for line in output.splitlines() if line.strip()]


def get_untracked_files():
    output = run_git(["ls-files", "--others", "--exclude-standard"])
    return [line.strip() for line in output.splitlines() if line.strip()]


def get_numstat(staged_only):
    if staged_only:
        output = run_git(["diff", "--cached", "--numstat"])
    else:
        output = run_git(["diff", "--numstat"])
    stats = {}
    for line in output.splitlines():
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        added, deleted, file_path = parts[0], parts[1], parts[2]
        stats[file_path] = (added, deleted)
    return stats


def map_java_to_md(java_path):
    java_prefix = "src/main/java/"
    if not java_path.startswith(java_prefix):
        return None
    if not java_path.endswith(".java"):
        return None
    if java_path.endswith("package-info.java"):
        return None
    relative_path = java_path[len(java_prefix):]
    md_path = "Project_Code_Wiki/" + relative_path[:-5] + ".md"
    return md_path


def format_stat(stat):
    if not stat:
        return "0/0"
    return f"{stat[0]}/{stat[1]}"


def get_file_stat(path_value, numstat, untracked_files):
    if path_value in untracked_files:
        return "new"
    return format_stat(numstat.get(path_value))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    repo_root = get_repo_root()
    os.chdir(repo_root)

    staged_files = [normalize_path(path) for path in get_staged_files()]
    working_tree_files = [normalize_path(path) for path in get_working_tree_files()]
    untracked_files = [normalize_path(path) for path in get_untracked_files()]
    use_staged = bool(staged_files)
    if not staged_files and not working_tree_files and not untracked_files:
        print("没有检测到暂存区、工作区或未跟踪变更")
        return

    if use_staged:
        changed_files = staged_files
        print("对比范围：暂存区")
    else:
        changed_files = list(dict.fromkeys(working_tree_files + untracked_files))
        print("对比范围：工作区（未暂存）")

    numstat = get_numstat(use_staged)

    java_files = [path for path in changed_files if path.startswith("src/main/java/") and path.endswith(".java") and not path.endswith("package-info.java")]
    md_files = set(path for path in changed_files if path.startswith("Project_Code_Wiki/") and path.endswith(".md"))

    issues = []

    print("影子文档对比结果")
    print("java_file | java_diff | md_file | md_exists | md_staged | md_diff")
    for java_file in java_files:
        md_file = map_java_to_md(java_file)
        md_exists = "否"
        md_staged = "否"
        md_diff = "0/0"
        if md_file:
            md_full = os.path.join(repo_root, md_file)
            md_exists = "是" if os.path.exists(md_full) else "否"
            md_staged = "是" if md_file in md_files else "否"
            md_diff = get_file_stat(md_file, numstat, set(untracked_files))
        java_diff = get_file_stat(java_file, numstat, set(untracked_files))
        print(f"{java_file} | {java_diff} | {md_file or '-'} | {md_exists} | {md_staged} | {md_diff}")
        if md_file is None:
            issues.append((java_file, "未找到影子文档映射"))
        elif md_exists == "否":
            issues.append((java_file, "影子文档不存在"))
        elif md_staged == "否":
            issues.append((java_file, "影子文档未跟随修改"))

    if issues:
        print("\n需要关注的文件")
        for java_file, reason in issues:
            print(f"{java_file} | {reason}")
        if args.strict:
            sys.exit(1)


if __name__ == "__main__":
    main()
