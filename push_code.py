import subprocess
import os

os.chdir(r'd:\DU_AN_BAP\PointTrack\PointTrack_BE')

try:
    # Stage all changes
    result = subprocess.run(['git', 'add', '-A'], capture_output=True, text=True)
    print(f"git add: {result.stdout} {result.stderr}")
    
    # Commit changes
    result = subprocess.run(['git', 'commit', '-m', 'Fix: Login 500 error - lazy loading and lazy initialization issues resolved'], capture_output=True, text=True)
    print(f"git commit: {result.stdout} {result.stderr}")
    
    # Push to origin Nghe
    result = subprocess.run(['git', 'push', 'origin', 'Nghe'], capture_output=True, text=True)
    print(f"git push: {result.stdout} {result.stderr}")
    
    # Show status
    result = subprocess.run(['git', 'status'], capture_output=True, text=True)
    print(f"git status: {result.stdout}")
    
    print("✅ Push successful!")
except Exception as e:
    print(f"❌ Error: {e}")
