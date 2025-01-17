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
            "hosts": ["jenkins", "nexus"],
            "children": {
                "jenkins": {
                    "hosts": ["jenkins"]
                },
                "nexus": {
                    "hosts": ["nexus"]
                }
            }
        },
        "_meta": {
            "hostvars": {
                "jenkins": {
                    "ansible_host": jenkins_ip,
                    "ansible_user": "root",
                    "ansible_ssh_private_key_file": "/root/.ssh/id_rsa"
                },
                "nexus": {
                    "ansible_host": nexus_ip,
                    "ansible_user": "root",
                    "ansible_ssh_private_key_file": "/root/.ssh/id_rsa"
                }
            }
        }
    }

    return inventory

def get_host_details(host_name):
    inventory = generate_inventory()
    return inventory["_meta"]["hostvars"].get(host_name, {})

def main():
    if len(sys.argv) > 1 and sys.argv[1] == "--list":
        try:
            inventory = generate_inventory()
            print(json.dumps(inventory, indent=2))
        except Exception as e:
            print(json.dumps({"_meta": {"hostvars": {}}}))
    elif len(sys.argv) > 2 and sys.argv[1] == "--host":
        host_name = sys.argv[2]
        try:
            host_details = get_host_details(host_name)
            print(json.dumps(host_details, indent=2))
        except Exception:
            print(json.dumps({}))
    else:
        print(json.dumps({"_meta": {"hostvars": {}}}))

if __name__ == "__main__":
    main()






