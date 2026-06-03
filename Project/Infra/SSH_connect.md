# SSH Connection Information

## Server 1 (GPU 4090 - Instance 39265510)
- **Public IP**: 194.14.47.19
- **Machine Copy Port**: 22099
- **Direct SSH Port**: 22059
- **Proxy SSH Port**: 25511 (ssh3.vast.ai)
- **Local IPs**: 192.168.50.10, 172.17.0.1
- **Username**: root (기본값)
- **Password/Key**: `~/.ssh/id_ed25519` (SSH 키 사용, 비밀번호 없음)

### 접속 명령어 (터미널/PowerShell 입력)
**Direct SSH Connect (권장):**
```bash
ssh -p 22059 root@194.14.47.19 -L 8080:localhost:8080
```

**Proxy SSH Connect (Direct가 막혔을 때):**
```bash
ssh -p 25511 root@ssh3.vast.ai -L 8080:localhost:8080
```

