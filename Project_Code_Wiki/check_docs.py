import os

def check_missing_docs():
    # Define roots relative to the script location
    # Script is expected to be in ai_kef/Project_Code_Wiki/
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # project_root is one level up (ai_kef/)
    project_root = os.path.dirname(script_dir)
    
    src_root = os.path.join(project_root, 'src', 'main', 'java', 'com', 'example', 'aikef')
    
    # doc_root is inside the script directory (Project_Code_Wiki/com/...)
    doc_root = os.path.join(script_dir, 'com', 'example', 'aikef')

    print(f"Scanning source: {src_root}")
    print(f"Checking docs in: {doc_root}")

    missing_docs = []

    for root, dirs, files in os.walk(src_root):
        for file in files:
            if file.endswith(".java") and file != "package-info.java":
                # Get relative path from src_root
                rel_path = os.path.relpath(root, src_root)
                
                # Construct expected doc path
                if rel_path == ".":
                    doc_subdir = doc_root
                else:
                    doc_subdir = os.path.join(doc_root, rel_path)
                
                doc_filename = file.replace(".java", ".md")
                doc_path = os.path.join(doc_subdir, doc_filename)

                if not os.path.exists(doc_path):
                    # Store relative path for cleaner output
                    full_rel_path = os.path.join(rel_path, file)
                    if rel_path == ".":
                        full_rel_path = file
                    missing_docs.append(full_rel_path)

    print(f"\nFound {len(missing_docs)} missing documentation files:")
    for item in sorted(missing_docs):
        print(f"- {item}")

if __name__ == "__main__":
    check_missing_docs()
