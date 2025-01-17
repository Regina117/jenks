#!/usr/bin/env python

import json
import subprocess
import sys

def get_terraform_output(output_name):
    command = ["terraform", "output", "-raw", output_name]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"Failed to get Terraform output for {output_name}: {result.stderr.strip()}")
    return result.stdout.strip()

def generate_inventory():
    jenkins_ip = get_terraform_output("jenkins_ip")
    nexus_ip = get_terraform_output("nexus_ip")

    inventory = {
        "all": {
            "children": {
                "jenkins": {
                    "hosts": [jenkins_ip]
                },
                "nexus": {
                    "hosts": [nexus_ip]
                }
            }
        },
        "_meta": {
            "hostvars": {
                jenkins_ip: {
                    "ansible_host": jenkins_ip,
                    "ansible_user": "root",
                    "ansible_ssh_private_key_file": "/root/.ssh/id_rsa"
                },
                nexus_ip: {
                    "ansible_host": nexus_ip,
                    "ansible_user": "root",
                    "ansible_ssh_private_key_file": "/root/.ssh/id_rsa"
                }
            }
        }
    }

    return inventory

inventory = generate_inventory()
print(json.dumps(inventory, indent=2))

if __name__ == "__main__":
    main()






