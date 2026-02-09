# 🟢 CarStoreView (Sales Service) — Serviço de Vendas

Este repositório contém o **CarStoreView**, o microsserviço dedicado ao **fluxo de venda e consultas de venda** (listagens), desenvolvido em **Spring Boot** seguindo **Clean Architecture**, com testes automatizados e pipeline completo de **CI/CD**.

> **Responsabilidades deste microserviço (Sales Service):**
> - **Efetuar a venda** de um veículo (fluxo de compra).
> - **Listar veículos disponíveis e vendidos**, ordenando por preço (do mais barato para o mais caro).
> - **Receber o webhook de pagamento** (confirmação/cancelamento), atualizando o estado da venda.
> - **Comunicar-se com o Core Service via HTTP** para sincronizar/consultar informações necessárias ao domínio.
>
> O **cadastro/edição de veículos** e a **gestão de usuários/roles** ficam no **Core Service** (outro repositório), com banco segregado.

---

## 📦 FASE 4 — Contexto do Projeto

A plataforma foi projetada para suportar aumento repentino de chamadas, isolando endpoints de **listagem** e **compra** em um serviço único (**CarStoreView**), com **banco de dados segregado**.

A comunicação entre serviços ocorre via **HTTP REST**, respeitando os limites de responsabilidade de cada componente.

---

## 🧱 Clean Architecture

Estrutura em camadas:

```
adapter   → Controllers e interfaces externas
usecase   → Regras de negócio
data      → DTOs e persistência
infra     → Configurações (DB, segurança, clientes HTTP)
```

### 📐 Diagrama (Clean Architecture)

```plantuml
@startuml
actor User
User -> Controller
Controller -> UseCase
UseCase -> Repository
Repository -> Database
@enduml
```

---

## 🏗️ Microsserviços (visão macro)

```plantuml
@startuml
actor Cliente

rectangle "Core Service" as core
rectangle "CarStoreView (Sales Service)" as view

database "DB Core" as dbcore
database "DB View" as dbview

Cliente -> core : Cadastro/edição
Usuários e veículos
core -> dbcore

core -> view : HTTP REST
(solicita venda/sincronização)
view -> dbview
view -> core : HTTP REST
(notifica/consulta Core)
@enduml
```

---

## ▶️ Executando Localmente

### ✅ Pré-requisitos
- Java 24
- Docker + Docker Compose
- Git

### 🐳 Rodar com Docker (recomendado)

```bash
docker compose up --build
```

### 💻 Rodar sem Docker (banco em Docker + app local)

Subir somente o banco (ajuste o nome do serviço conforme seu compose):
```bash
docker compose up db -d
```

Subir a aplicação local:
```bash
./mvnw spring-boot:run
```

### 📄 Variáveis de ambiente (mínimo)

Para rodar local, estas variáveis precisam estar configuradas (exemplo):

```bash
export SERVER_PORT=8081
export DB_URL="jdbc:postgresql://localhost:5433/carstore_view"
export DB_USER="view_user"
export DB_PASS="view_pass"
export FLYWAY_ENABLED=true
export JPA_DDL_AUTO=validate
export CORE_BASE_URL="http://localhost:8080"
export JWT_PUBLIC_KEY_PATH="../CarStoreBack/keys/public_key.pem"
```

---

## 📖 Swagger / OpenAPI

Após subir a aplicação, acesse:

- **Swagger UI:** `http://localhost:8081/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

---

## 🧪 Testes

```bash
./mvnw test
```

---

## 🔄 CI/CD (GitHub Actions)

Este repositório utiliza GitHub Actions para:

- Executar testes automaticamente
- Analisar qualidade (SonarCloud)
- Build Docker
- Push da imagem para Amazon ECR
- Deploy automático na EC2 com Docker Compose

### ✅ Gatilho do pipeline

O workflow é disparado quando ocorre **push/merge na branch `main`**:
- **Merge de Pull Request → main** (recomendado)
- **Push direto → main** (também dispara)

Fluxo (alto nível):
```
push na main → testes → sonar → build docker → push ECR → deploy EC2
```

---

## 🔐 Criar Secrets via CLI (gh) para o Actions

### 1) Autenticar
```bash
gh auth login
```

### 2) Criar secrets manualmente (exemplo)
```bash
gh secret set SERVER_PORT --body "8081"
gh secret set CORE_BASE_URL --body "http://localhost:8080"
```

### 3) Importar em lote a partir do `.env` (recomendado)
Se você tiver o script `import-secrets.sh`:
```bash
chmod +x import-secrets.sh
./import-secrets.sh
```

---

## 🔐 Nota de Segurança (Contexto Acadêmico)

Este projeto utiliza **chave pública JWT** para validação de tokens.  
No contexto acadêmico, arquivos de chave usados apenas para desenvolvimento podem existir no repositório, mas **não devem ser usados em produção**.

---

## 📚 Tecnologias
- Java 24
- Spring Boot
- PostgreSQL
- Docker / Docker Compose
- PlantUML
- GitHub Actions
- SonarCloud
- JUnit + Mockito

---

## 👨‍💻 Autor

Leandro Shiniti Tacara  
RM355388  
Pós Tech FIAP — Turma SOAT7
