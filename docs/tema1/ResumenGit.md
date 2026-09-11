# Tabla Resumen de Comandos Git Esenciales

## 🔧 Configuración Inicial

| Comando | Descripción |
|---------|-------------|
| `git config --global user.name "Nombre"` | Configura tu nombre de usuario |
| `git config --global user.email "email"` | Configura tu correo electrónico |
| `git config --list` | Muestra la configuración actual |
| `git init` | Inicializa un repositorio Git en el directorio actual |
| `git clone <url>` | Clona un repositorio remoto |

## 📝 Gestión de Cambios (Área de Trabajo)

| Comando | Descripción |
|---------|-------------|
| `git status` | Muestra el estado de los archivos |
| `git add <archivo>` | Añade un archivo al área de staging |
| `git add .` | Añade todos los cambios al staging |
| `git add -p` | Añade cambios de forma interactiva (por fragmentos) |
| `git rm <archivo>` | Elimina un archivo del repo y del disco |
| `git mv <origen> <destino>` | Mueve o renombra un archivo |

## 💾 Confirmar Cambios (Commits)

| Comando | Descripción |
|---------|-------------|
| `git commit -m "mensaje"` | Crea un commit con mensaje |
| `git commit -am "mensaje"` | Añade y hace commit de archivos ya rastreados |
| `git commit --amend` | Modifica el último commit |
| `git log` | Muestra el historial de commits |
| `git log --oneline --graph --all` | Historial resumido y gráfico |

## 🌿 Ramas (Branches)

| Comando | Descripción |
|---------|-------------|
| `git branch` | Lista las ramas locales |
| `git branch -a` | Lista ramas locales y remotas |
| `git branch <nombre>` | Crea una nueva rama |
| `git checkout <rama>` | Cambia a otra rama |
| `git checkout -b <rama>` | Crea y cambia a una nueva rama |
| `git switch <rama>` | Cambia de rama (comando moderno) |
| `git switch -c <rama>` | Crea y cambia de rama (moderno) |
| `git merge <rama>` | Fusiona una rama con la actual |
| `git branch -d <rama>` | Elimina una rama |
| `git rebase <rama>` | Reaplica commits sobre otra base |

## 🔄 Sincronización con Remoto

| Comando | Descripción |
|---------|-------------|
| `git remote -v` | Lista los repositorios remotos |
| `git remote add origin <url>` | Añade un remoto |
| `git fetch` | Descarga cambios sin fusionar |
| `git pull` | Descarga y fusiona cambios del remoto |
| `git pull --rebase` | Descarga y reaplica tus cambios |
| `git push` | Sube cambios al remoto |
| `git push -u origin <rama>` | Sube y establece upstream |
| `git push origin --delete <rama>` | Elimina una rama remota |

## ↩️ Deshacer Cambios

| Comando | Descripción |
|---------|-------------|
| `git restore <archivo>` | Descarta cambios en el working dir |
| `git restore --staged <archivo>` | Quita del staging |
| `git reset --soft HEAD~1` | Deshace el último commit (mantiene cambios) |
| `git reset --hard HEAD~1` | Deshace commit y cambios ⚠️ |
| `git revert <commit>` | Crea un commit que deshace otro |
| `git stash` | Guarda cambios temporalmente |
| `git stash pop` | Recupera cambios guardados |
| `git stash list` | Lista los stash guardados |

## 🏷️ Etiquetas (Tags)

| Comando | Descripción |
|---------|-------------|
| `git tag` | Lista etiquetas |
| `git tag <nombre>` | Crea una etiqueta |
| `git tag -a <nombre> -m "msg"` | Etiqueta anotada |
| `git push origin <tag>` | Sube una etiqueta |
| `git push origin --tags` | Sube todas las etiquetas |

## 🔍 Inspección y Búsqueda

| Comando | Descripción |
|---------|-------------|
| `git diff` | Diferencias sin staging |
| `git diff --staged` | Diferencias en staging |
| `git show <commit>` | Muestra detalles de un commit |
| `git blame <archivo>` | Quién cambió cada línea |
| `git grep "texto"` | Busca texto en el repo |

## 🚀 Comandos Avanzados Útiles

| Comando | Descripción |
|---------|-------------|
| `git cherry-pick <commit>` | Aplica un commit específico a la rama actual |
| `git reflog` | Historial de referencias (recuperar commits) |
| `git bisect` | Encuentra el commit que introdujo un bug |
| `git clean -fd` | Elimina archivos no rastreados ⚠️ |
| `git submodule` | Gestiona submódulos |

---

### 💡 Consejos rápidos
- **`.gitignore`**: archivo para excluir archivos del control de versiones.
- **⚠️ `reset --hard` y `clean -fd`**: irreversibles, úsalos con cuidado.
- **Flujo típico**: `git add .` → `git commit -m "msg"` → `git push`.

¿Quieres que profundice en alguno de estos comandos o en un flujo de trabajo específico (Git Flow, trunk-based, etc.)?
