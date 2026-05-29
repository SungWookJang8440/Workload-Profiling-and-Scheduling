import docker
import uuid
import logging
from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel
from typing import List, Optional

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="GPU Sharing Worker API", version="1.0.0")

# Docker 클라이언트 초기화 (호스트의 Docker 소켓을 사용)
try:
    client = docker.from_env()
except Exception as e:
    logger.error(f"Failed to connect to Docker daemon: {e}")
    client = None

class CreateContainerRequest(BaseModel):
    image_name: str
    container_id: str
    ssh_password: Optional[str] = "password"
    # 추가로 필요한 할당 자원(MIG UUID 등)을 받을 수 있음
    gpu_device: Optional[str] = "0:1" # 예: MIG 디바이스 지정 

class ContainerResponse(BaseModel):
    container_id: str
    status: str
    message: Optional[str] = None

@app.get("/health")
def health_check():
    if not client:
        raise HTTPException(status_code=500, detail="Docker client not initialized")
    return {"status": "ok", "docker_connected": True}

@app.get("/containers")
def list_containers():
    if not client:
        raise HTTPException(status_code=500, detail="Docker client not initialized")
    try:
        containers = client.containers.list(all=True, filters={"label": "gpu-sharing=worker"})
        result = []
        for c in containers:
            result.append({
                "id": c.id,
                "name": c.name,
                "status": c.status,
                "image": c.image.tags[0] if c.image.tags else "unknown"
            })
        return {"containers": result}
    except Exception as e:
        logger.error(f"Error listing containers: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/containers/create", response_model=ContainerResponse)
def create_container(req: CreateContainerRequest):
    if not client:
        raise HTTPException(status_code=500, detail="Docker client not initialized")
    
    try:
        # GPU 옵션 구성 (NVIDIA Container Toolkit 필요)
        device_request = docker.types.DeviceRequest(
            device_ids=[req.gpu_device],  # 예: "0:1" (MIG 인스턴스)
            capabilities=[['gpu']]
        )
        
        # 실제 컨테이너 생성 및 실행
        # 참고: 실 서비스에서는 포트 매핑(jupyter, ssh) 등을 동적으로 할당해야 함
        container = client.containers.run(
            image=req.image_name,
            name=f"gpu-sharing-{req.container_id}",
            detach=True,
            device_requests=[device_request],
            labels={"gpu-sharing": "worker", "gpu-sharing-id": req.container_id},
            environment={"PASSWORD": req.ssh_password},
            # command="jupyter notebook --ip 0.0.0.0 --allow-root", # 테스트용 커맨드
            tty=True
        )
        
        logger.info(f"Container created successfully: {container.id}")
        return ContainerResponse(
            container_id=container.id,
            status="CREATED",
            message="Container successfully created."
        )
    except docker.errors.ImageNotFound:
        raise HTTPException(status_code=404, detail=f"Image {req.image_name} not found")
    except Exception as e:
        logger.error(f"Error creating container: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.delete("/containers/{container_id}")
def delete_container(container_id: str):
    if not client:
        raise HTTPException(status_code=500, detail="Docker client not initialized")
    
    try:
        # 이름 기반 또는 ID 기반 검색
        containers = client.containers.list(all=True, filters={"label": f"gpu-sharing-id={container_id}"})
        if not containers:
            raise HTTPException(status_code=404, detail="Container not found")
            
        for c in containers:
            logger.info(f"Stopping and removing container: {c.id}")
            c.stop(timeout=10)
            c.remove(force=True)
            
        return {"status": "DELETED", "message": f"Container {container_id} removed"}
    except Exception as e:
        logger.error(f"Error deleting container: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
