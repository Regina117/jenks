#!/usr/bin/env python3
import json
import subprocess

def get_terraform_outputs():
    try:
        result = subprocess.run(
            ["terraform", "output", "-json"],
            capture_output=True,
            text=True,
            check=True
        )
        return json.loads(result.stdout)
    except Exception as e:
        print(f"Error: {e}")
        exit(1)

def generate_inventory(outputs):
    inventory = {
        "all": {
            "hosts": [],
            "children": {
                "jenkins": {"hosts": []},
                "nexus": {"hosts": []}
            }
        }
    }


    if "jenkins_ip" in outputs and outputs["jenkins_ip"]["value"]:
        jenkins_ip = outputs["jenkins_ip"]["value"]
        inventory["all"]["hosts"].append(jenkins_ip)
        inventory["all"]["children"]["jenkins"]["hosts"].append(jenkins_ip)
        inventory["_meta"] = {
            "hostvars": {
                jenkins_ip: {
                    "ansible_user": "root",
                    "ansible_ssh_private_key_file": "/root/.ssh/id_rsa"
                }
            }
        }

   
    if "nexus_ip" in outputs and outputs["nexus_ip"]["value"]:
        nexus_ip = outputs["nexus_ip"]["value"]
        inventory["all"]["hosts"].append(nexus_ip)
        inventory["all"]["children"]["nexus"]["hosts"].append(nexus_ip)
        inventory["_meta"] = {
            "hostvars": {
                jenkins_ip: {
                    "ansible_user": "root",
                    "ansible_ssh_private_key_file": "/root/.ssh/id_rsa"
                }
            }
        }

    return inventory

if __name__ == "__main__":
    terraform_outputs = get_terraform_outputs()
    inventory = generate_inventory(terraform_outputs)
    print(json.dumps(inventory, indent=2))







