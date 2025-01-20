terraform {
  required_providers {
    yandex = {
      source = "yandex-cloud/yandex"
    }
  }
  required_version = ">= 0.13"
}

provider "yandex" {
  token     = "${var.yc_token}"
  cloud_id  = "${var.yc_cloud_id}"
  folder_id = "${var.yc_folder_id}"
  zone      = "ru-central1-d"
}

resource "yandex_compute_instance" "jenkins" {
  name        = "jenkins"
  platform_id = "standard-v2" 
  resources {
    cores  = 8
    memory = 8
  }
  boot_disk {
    initialize_params {
      image_id = "fd8421uec874p44sq4j3" 
      size     = 40
    }
  }
  network_interface {
    subnet_id = var.subnet_id
    nat       = true
  }

  metadata = {
    ssh-keys= "regina:${file("/home/regina/.ssh/id_rsa.pub")}"
    user-data = <<-EOT
      #cloud-config
      users:
      - name: regina
        sudo: ALL=(ALL) NOPASSWD:ALL
        groups: sudo
        ssh_authorized_keys:
          - ${file("/home/regina/.ssh/id_rsa.pub")}
      runcmd:
        - apt update && apt install -y openssh-server
        - systemctl enable ssh
        - systemctl start ssh
    EOT
  }
}

resource "yandex_compute_instance" "prod" {
  name        = "prod"
  platform_id = "standard-v2"
  resources {
    cores  = 4
    memory = 4
  }
  boot_disk {
    initialize_params {
      image_id = "fd8421uec874p44sq4j3"
      size     = 35
    }
  }
  network_interface {
    subnet_id = var.subnet_id
    nat       = true
  }

  metadata = {
    ssh-keys= "regina:${file("/home/regina/.ssh/id_rsa.pub")}"
    user-data = <<-EOT
      #cloud-config
      users:
      - name: regina
        sudo: ALL=(ALL) NOPASSWD:ALL
        groups: sudo
        ssh_authorized_keys:
          - ${file("/home/regina/.ssh/id_rsa.pub")}
      runcmd:
        - apt update && apt install -y openssh-server
        - systemctl enable ssh
        - systemctl start ssh
    EOT
  }
}

resource "yandex_compute_instance" "nexus" {
  name        = "nexus"
  platform_id = "standard-v2"
  resources {
    cores  = 4
    memory = 4
  }
  boot_disk {
    initialize_params {
      image_id = "fd8421uec874p44sq4j3"
      size     = 35
    }
  }
  network_interface {
    subnet_id = var.subnet_id
    nat       = true
  }

  metadata = {
    ssh-keys= "regina:${file("/home/regina/.ssh/id_rsa.pub")}"
    user-data = <<-EOT
      #cloud-config
      users:
      - name: regina
        sudo: ALL=(ALL) NOPASSWD:ALL
        groups: sudo
        ssh_authorized_keys:
          - ${file("/home/regina/.ssh/id_rsa.pub")}
      runcmd:
        - apt update && apt install -y openssh-server
        - systemctl enable ssh
        - systemctl start ssh
    EOT
  }
}

variable "yc_token" {}
variable "yc_cloud_id" {}
variable "yc_folder_id" {}
variable "subnet_id" {}

output "jenkins_ip" {
  value = yandex_compute_instance.jenkins.network_interface[0].nat_ip_address
}

output "prod_ip" {
  value = yandex_compute_instance.prod.network_interface[0].nat_ip_address
}

output "nexus_ip" {
  value = yandex_compute_instance.nexus.network_interface[0].nat_ip_address
}
