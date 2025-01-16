#!/bin/bash

# Получение данных из Terraform
MASTER_IP=$(terraform output -raw master_ip)
NEXUS_IP=$(terraform output -raw nexus_ip)

# Генерация Ansible инвентаря
cat <<EOF > inventory.yml
all:
  children:
    jenkins:
      hosts:
        master-jenkins:
          ansible_host: $MASTER_IP
          ansible_user: regina
          ansible_ssh_private_key_file: /path/to/private/key
    nexus:
      hosts:
        nexus:
          ansible_host: $NEXUS_IP
          ansible_user: regina
          ansible_ssh_private_key_file: /path/to/private/key
EOF


