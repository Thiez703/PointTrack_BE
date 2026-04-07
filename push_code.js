const { execSync } = require('child_process');
const path = require('path');

const repoPath = path.resolve('d:\\DU_AN_BAP\\PointTrack\\PointTrack_BE');

try {
    console.log('📁 Changing to:', repoPath);
    process.chdir(repoPath);
    
    console.log('\n📝 Staging changes...');
    execSync('git add -A', { stdio: 'inherit' });
    
    console.log('\n💾 Committing changes...');
    execSync('git commit -m "Fix: Login 500 error - lazy loading and lazy initialization issues resolved"', { stdio: 'inherit' });
    
    console.log('\n🚀 Pushing to origin/Nghe...');
    execSync('git push origin Nghe', { stdio: 'inherit' });
    
    console.log('\n✅ Push successful!');
    console.log('\n📊 Current status:');
    execSync('git status', { stdio: 'inherit' });
} catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
}
