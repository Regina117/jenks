#!/usr/bin/env python

import json
import subprocess

def get_terraform_output(output_name):
    command = ["terraform", "output", "-raw", output_name]
    result = subprocess.run(command, capture_output=True, text=True)
    return result.stdout.strip()

def generate_inventory():
    inventory = {
        "all": {
            "children": {
                "master": {
                    "hosts": {
                        "master-jenkins": {
                            "ansible_host": get_terraform_output("master_ip"),
                            "ansible_user": "regina",
                            "ansible_ssh_private_key_file": "/home/regina/.ssh/id_rsa.pub"
                        }
                    }
                },
                "nexus": {
                    "hosts": {
                        "nexus": {
                            "ansible_host": get_terraform_output("nexus_ip"),
                            "ansible_user": "regina",
                            "ansible_ssh_private_key_file": "/home/regina/.ssh/id_rsa.pub"
                        }
                    }
                }
            }
        }
    }

    return inventory

def main():
    inventory = generate_inventory()
    print(json.dumps(inventory, indent=2))

if __name__ == "__main__":
    main()
