import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    ssh.connect('155.230.118.52', port=22345, username='sslab', password='sslab1!2', timeout=5)
    stdin, stdout, stderr = ssh.exec_command("nvidia-smi --help-query-mig")
    print(stdout.read().decode())
    print("ERR:", stderr.read().decode())
finally:
    ssh.close()
