#!/usr/bin/env bash
set -euo pipefail

USER_NAME="${SUDO_USER:-$USER}"

echo "🔎 Detectando sistema operacional..."

if [ -f /etc/os-release ]; then
  . /etc/os-release
  OS=$ID
else
  echo "❌ Não foi possível detectar o sistema."
  exit 1
fi

echo "✅ Sistema detectado: $OS"
echo "👤 Usuário alvo: $USER_NAME"

install_amazon_linux() {
  echo "🚀 Instalando Docker no Amazon Linux..."
  sudo dnf update -y || sudo yum update -y
  sudo dnf install -y docker || sudo yum install -y docker

  sudo systemctl enable docker
  sudo systemctl start docker

  echo "📦 Instalando Docker Compose v2 (plugin)..."
  sudo mkdir -p /usr/local/lib/docker/cli-plugins/

  # usa latest oficial do GitHub
  sudo curl -fsSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose

  sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
}

install_ubuntu() {
  echo "🚀 Instalando Docker no Ubuntu..."
  sudo apt update -y
  sudo apt install -y ca-certificates curl gnupg lsb-release

  sudo mkdir -p /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

  sudo apt update -y
  sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

  sudo systemctl enable docker
  sudo systemctl start docker
}

case "$OS" in
  amzn)   install_amazon_linux ;;
  ubuntu) install_ubuntu ;;
  *)
    echo "❌ Distro não suportada automaticamente: $OS"
    exit 1
    ;;
esac

echo "🔧 Configurando permissões para usar Docker sem sudo..."

# cria grupo docker (se não existir)
sudo groupadd docker 2>/dev/null || true

# adiciona usuário ao grupo docker
sudo usermod -aG docker "$USER_NAME"

# garante que o socket pertence ao grupo docker (após docker iniciar)
sudo systemctl restart docker || true

if [ -S /var/run/docker.sock ]; then
  sudo chown root:docker /var/run/docker.sock || true
  sudo chmod 660 /var/run/docker.sock || true
fi

echo "🧪 Verificando instalação..."
docker --version || true

# compose pode estar como plugin (docker compose) ou binário legacy (docker-compose)
if docker compose version >/dev/null 2>&1; then
  docker compose version
elif command -v docker-compose >/dev/null 2>&1; then
  docker-compose --version
else
  echo "⚠️ Docker Compose não encontrado no PATH (mas pode estar instalado como plugin)."
fi

echo "👥 Grupo docker no sistema:"
getent group docker || true

echo "👥 Grupos do usuário $USER_NAME:"
id -nG "$USER_NAME" || true

# tenta abrir um subshell com o grupo docker (não muda seu shell pai, mas ajuda)
if id -nG "$USER_NAME" | grep -qw docker; then
  echo "✅ Usuário $USER_NAME já está no grupo docker."
else
  echo "❌ Usuário $USER_NAME ainda não aparece no grupo docker."
  echo "   Rode novamente: sudo usermod -aG docker $USER_NAME"
fi

echo
echo "✅ Próximo passo (necessário para a sessão atual reconhecer o grupo):"
echo "   1) Saia da sessão e conecte novamente (recomendado): exit  &&  ssh ..."
echo "   OU"
echo "   2) Abra um subshell com o grupo docker:"
echo "      newgrp docker"
echo
echo "Depois teste:"
echo "   docker ps"
echo "   docker run hello-world"
