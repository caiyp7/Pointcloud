# 部署指南

本指南说明如何将项目上传到远程 Git 仓库，以便团队成员可以克隆并直接部署。

## 上传到 GitHub

### 1. 在 GitHub 上创建新仓库

1. 访问 [GitHub](https://github.com)
2. 点击右上角的 `+` 号，选择 `New repository`
3. 填写仓库名称（例如：`PointCloud`）
4. **不要**勾选 "Initialize this repository with a README"
5. 点击 `Create repository`

### 2. 添加远程仓库并推送

```bash
cd /home/cyp/AndroidStudioProjects/PointCloud

# 添加远程仓库（将 YOUR_USERNAME 替换为你的 GitHub 用户名）
git remote add origin https://github.com/YOUR_USERNAME/PointCloud.git

# 推送到远程仓库
git push -u origin master
```

### 3. 团队成员克隆和部署

团队成员可以通过以下步骤克隆并部署：

```bash
# 克隆项目（包括 submodules）
git clone --recursive https://github.com/YOUR_USERNAME/PointCloud.git

# 进入项目目录
cd PointCloud

# 如果忘记使用 --recursive，可以执行以下命令初始化 submodules
git submodule update --init --recursive

# 使用 Android Studio 打开项目或使用命令行构建
./gradlew build
```

## 上传到 GitLab

### 1. 在 GitLab 上创建新项目

1. 访问 [GitLab](https://gitlab.com)
2. 点击 `New project` -> `Create blank project`
3. 填写项目名称
4. 取消勾选 "Initialize repository with a README"
5. 点击 `Create project`

### 2. 添加远程仓库并推送

```bash
cd /home/cyp/AndroidStudioProjects/PointCloud

# 添加远程仓库
git remote add origin https://gitlab.com/YOUR_USERNAME/PointCloud.git

# 推送到远程仓库
git push -u origin master
```

### 3. 团队成员克隆

```bash
git clone --recursive https://gitlab.com/YOUR_USERNAME/PointCloud.git
cd PointCloud
git submodule update --init --recursive
```

## 上传到 Gitee（码云）

### 1. 在 Gitee 上创建新仓库

1. 访问 [Gitee](https://gitee.com)
2. 点击右上角的 `+` 号，选择 `新建仓库`
3. 填写仓库名称
4. **不要**勾选 "使用 Readme 文件初始化这个仓库"
5. 点击 `创建`

### 2. 添加远程仓库并推送

```bash
cd /home/cyp/AndroidStudioProjects/PointCloud

# 添加远程仓库
git remote add origin https://gitee.com/YOUR_USERNAME/PointCloud.git

# 推送到远程仓库
git push -u origin master
```

### 3. 团队成员克隆

```bash
git clone --recursive https://gitee.com/YOUR_USERNAME/PointCloud.git
cd PointCloud
git submodule update --init --recursive
```

## 使用 SSH 方式（推荐）

使用 SSH 方式可以避免每次推送都输入密码。

### 1. 生成 SSH 密钥（如果还没有）

```bash
ssh-keygen -t ed25519 -C "your_email@example.com"
# 按回车使用默认位置，可以设置密码或留空

# 查看公钥
cat ~/.ssh/id_ed25519.pub
```

### 2. 添加 SSH 公钥到远程仓库

- **GitHub**: Settings -> SSH and GPG keys -> New SSH key
- **GitLab**: Preferences -> SSH Keys
- **Gitee**: 设置 -> SSH 公钥

将 `id_ed25519.pub` 的内容复制粘贴到网站上。

### 3. 使用 SSH 地址添加远程仓库

```bash
# GitHub
git remote add origin git@github.com:YOUR_USERNAME/PointCloud.git

# GitLab
git remote add origin git@gitlab.com:YOUR_USERNAME/PointCloud.git

# Gitee
git remote add origin git@gitee.com:YOUR_USERNAME/PointCloud.git

# 推送
git push -u origin master
```

## 常用 Git 命令

### 查看状态

```bash
git status
```

### 添加更改

```bash
# 添加所有更改
git add .

# 添加特定文件
git add path/to/file
```

### 提交更改

```bash
git commit -m "描述你的更改"
```

### 推送到远程

```bash
git push
```

### 拉取最新代码

```bash
git pull
```

### 查看远程仓库

```bash
git remote -v
```

### 更改远程仓库地址

```bash
git remote set-url origin NEW_URL
```

## 团队协作最佳实践

### 1. 分支策略

创建功能分支进行开发：

```bash
# 创建并切换到新分支
git checkout -b feature/your-feature-name

# 开发完成后，推送分支
git push -u origin feature/your-feature-name

# 在远程仓库创建 Pull Request/Merge Request
```

### 2. 保持代码最新

在开始新工作前，先拉取最新代码：

```bash
git checkout master
git pull origin master
```

### 3. 提交信息规范

使用清晰的提交信息：

```bash
git commit -m "feat: 添加新功能"
git commit -m "fix: 修复 bug"
git commit -m "docs: 更新文档"
git commit -m "refactor: 重构代码"
```

## 注意事项

1. **不要提交敏感信息**
   - 密钥文件（*.jks, *.keystore）
   - API 密钥和密码
   - 本地配置文件（local.properties）

2. **检查 .gitignore**
   - 确保已正确配置，避免提交不必要的文件
   - 构建产物（build/）
   - IDE 配置（.idea/ 的部分文件）

3. **Submodules 使用**
   - 克隆时必须使用 `--recursive` 标志
   - 或在克隆后执行 `git submodule update --init --recursive`

4. **Android SDK 路径**
   - 每个开发者的 SDK 路径不同
   - Android Studio 会自动生成 `local.properties`
   - 该文件已在 `.gitignore` 中，不会被提交

## 故障排除

### 问题 1: 推送时提示权限错误

**解决方案**: 检查 SSH 密钥配置或使用 HTTPS 方式并输入正确的用户名和密码。

### 问题 2: submodule 未下载

**解决方案**:

```bash
git submodule update --init --recursive
```

### 问题 3: 推送大文件失败

**解决方案**: 
- 检查文件是否应该被忽略
- 对于大文件，考虑使用 Git LFS

```bash
# 安装 Git LFS
git lfs install

# 跟踪大文件类型
git lfs track "*.apk"
git lfs track "*.so"

# 添加 .gitattributes
git add .gitattributes
git commit -m "Add Git LFS configuration"
```

## 完整工作流程示例

```bash
# 1. 克隆项目
git clone --recursive https://github.com/YOUR_USERNAME/PointCloud.git
cd PointCloud

# 2. 创建功能分支
git checkout -b feature/new-feature

# 3. 进行开发工作
# ... 编写代码 ...

# 4. 查看更改
git status
git diff

# 5. 添加更改
git add .

# 6. 提交更改
git commit -m "feat: 添加新功能描述"

# 7. 推送到远程
git push -u origin feature/new-feature

# 8. 在远程仓库创建 Pull Request

# 9. 合并后，更新本地 master 分支
git checkout master
git pull origin master

# 10. 删除功能分支
git branch -d feature/new-feature
```

## 技术支持

如果遇到问题，请参考：

- [Git 官方文档](https://git-scm.com/doc)
- [GitHub 帮助](https://docs.github.com)
- [GitLab 文档](https://docs.gitlab.com)
- [Gitee 帮助中心](https://gitee.com/help)

