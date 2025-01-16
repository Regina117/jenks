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

resource "yandex_compute_instance" "master" {
  name        = "master-jenkins"
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
    user-data = <<-EOT
      #cloud-config
      runcmd:
        - apt update && apt upgrade -y
        - apt install -y openjdk-11-jdk maven docker.io
        - apt install -y jenkins ansible
        - systemctl enable jenkins && systemctl start jenkins
    EOT
  }
}

resource "yandex_compute_instance" "build_node" {
  count       = 2
  name        = "build-node-${count.index + 1}"
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
    user-data = <<-EOT
      #cloud-config
      runcmd:
        - apt update && apt upgrade -y
        - apt install -y openjdk-11-jdk maven
    EOT
  }
}

resource "yandex_compute_instance" "prod_node" {
  name        = "prod-node"
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
    user-data = <<-EOT
      #cloud-config
      runcmd:
        - apt update && apt upgrade -y
        - apt install -y openjdk-11-jdk
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
    user-data = <<-EOT
      #cloud-config
      runcmd:
        - apt update && apt upgrade -y
        - apt install -y docker.io
        - docker run -d -p 8081:8081 --name nexus sonatype/nexus3
    EOT
  }
}

variable "yc_token" {}
variable "yc_cloud_id" {}
variable "yc_folder_id" {}
variable "subnet_id" {}

output "master_ip" {
  value = yandex_compute_instance.master.network_interface[0].nat_ip_address
}

output "build_nodes_ips" {
  value = yandex_compute_instance.build_node[*].network_interface[0].nat_ip_address
}

output "prod_node_ip" {
  value = yandex_compute_instance.prod_node.network_interface[0].nat_ip_address
}

output "nexus_ip" {
  value = yandex_compute_instance.nexus.network_interface[0].nat_ip_address
}
