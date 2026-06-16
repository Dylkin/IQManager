const fs = require('fs');
const path = require('path');

const sourceDir = '/tmp/angular-dist/browser';
const targetDir = path.join(__dirname, '..', 'java-backend', 'src', 'main', 'resources', 'static');

function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function copyRecursive(src, dest) {
  ensureDir(dest);
  const entries = fs.readdirSync(src, { withFileTypes: true });
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyRecursive(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

// Clean target directory first (except maybe keep some backend-specific files if any)
if (fs.existsSync(targetDir)) {
  fs.rmSync(targetDir, { recursive: true, force: true });
}

if (!fs.existsSync(sourceDir)) {
  console.error(`Source directory not found: ${sourceDir}`);
  process.exit(1);
}

copyRecursive(sourceDir, targetDir);
console.log(`Copied frontend build from ${sourceDir} to ${targetDir}`);
