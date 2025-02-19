# Сертификационный проект по итогам обучения в школе DevOps

Этот проект является итоговой работой по завершению обучения в школе DevOps. В рамках проекта были применены знания по CI/CD, IaC, Cloud, Docker, Linux и Networking. Проект позволил проработать навыки на примере реального цикла развертывания приложений.

## Ключевые этапы проекта

### 1. Подготовка инфраструктуры
- Создан аккаунт в Yandex Cloud.
- Установлены Terraform и Ansible для автоматизации управления инфраструктурой.

### 2. Развертывание виртуальных машин
- Написан конфигурационный файл `addvm.tf` для создания инфраструктуры, включающей три виртуальные машины: `jenkins`, `nexus` и `prod`.
- Создан файл переменных `terraform.tfvars` для параметризации конфигурации.

### 3. Запуск Terraform
- Скачаны необходимые файлы с GitHub.
- Выполнены команды для инициализации и развертывания инфраструктуры:
  ```bash
  terraform init
  terraform plan
  terraform validate
  terraform apply
  ```
- Настроено безопасное SSH-соединение между созданными виртуальными машинами для обеспечения их взаимодействия.

### 4. Динамическая инвентаризация для Ansible
- Разработан скрипт `terraform_inventory.py`, передающий IP-адреса виртуальных машин из Terraform в Ansible.
- Скрипт сделан исполняемым:
  ```bash
  chmod +x /home/regina/jenks/terraform_inventory.py
  ```

### 5. Настройка сервисов с помощью Ansible
- Создан Ansible Playbook `setup_services.yml` для автоматической установки и настройки `jenkins` и `nexus`.
- Запуск Playbook:
  ```bash
  ansible-playbook -i terraform_inventory.py setup_services.yml
  ```

### 6. Конфигурация Jenkins и Nexus
- На сервере `jenkins` установлены необходимые плагины и добавлены учетные данные (credentials).
- На сервере `nexus` создан репозиторий для хранения артефактов.

### 7. Настройка CI/CD пайплайнов
- Создан файл `Jenkinsfile`, реализующий сборку образа и загрузку его в Docker Registry, а также развертывание собранного образа из Registry на сервере `prod`.

## Схема реализации

![Схема реализации](https://github.com/Regina117/jenks/wiki)  

---

### Используемые технологии
- **CI/CD**: Jenkins
- **IaC**: Terraform
- **Cloud**: Yandex Cloud
- **Containerization**: Docker
- **Configuration Management**: Ansible
- **Networking**: SSH, TCP/IP

### Как запустить проект
1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/Regina117/jenks.git
   ```
2. Перейдите в директорию проекта:
   ```bash
   cd jenks
   ```
3. Инициализируйте Terraform:
   ```bash
   terraform init
   ```
4. Примените конфигурацию:
   ```bash
   terraform apply
   ```
5. Запустите Ansible Playbook:
   ```bash
   ansible-playbook -i terraform_inventory.py setup_services.yml
   ```

---




