import os
import sys
import requests

def main():
    # 환경 변수 로드
    gemini_key = os.environ.get("GEMINI_API_KEY")
    github_token = os.environ.get("GITHUB_TOKEN")
    pr_number = os.environ.get("PR_NUMBER")
    repo = os.environ.get("REPO_FULL_NAME")

    if not all([gemini_key, github_token, pr_number, repo]):
        print("필수 환경 변수가 누락되었습니다.")
        sys.exit(1)

    headers = {
        "Authorization": f"token {github_token}",
        "Accept": "application/vnd.github.v3.diff"  # 코드 변경점(Diff)을 텍스트로 가져오기 위한 설정
    }

    # 1. 깃허브로부터 PR 코드 변경점(Diff) 가져오기
    diff_url = f"https://api.github.com/repos/{repo}/pulls/{pr_number}"
    response = requests.get(diff_url, headers=headers)
    
    if response.status_code != 200:
        print("PR Diff를 가져오는데 실패했습니다.")
        sys.exit(1)
        
    pr_diff = response.text

    # 코드 변경량이 너무 많을 경우를 대비한 간단한 제한
    if len(pr_diff) > 40000:
        pr_diff = pr_diff[:40000] + "\n... (중략) ..."

    # 2. Gemini API 호출을 위한 프롬프트 및 페이로드 구성
    # 최신 모델인 gemini-2.5-flash를 사용합니다.
    gemini_url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={gemini_key}"
    
    prompt = (
        "너는 10년차 시니어 소프트웨어 엔지니어이자 까다로운 코드 리뷰어야.\n"
        "아래 제공되는 GitHub PR의 코드 변경사항(Diff)을 면밀히 분석하고, 한국어로 리뷰 리포트를 작성해줘.\n\n"
        "리포트에는 다음 내용이 반드시 포함되어야 해:\n"
        "1. 중복성 분석: 기존 시스템이나 일반적인 로직과 중복되는 부분이 있는지 분석\n"
        "2. 충돌 및 버그 가능성 분석: 로직상 오류, 예외 처리 누락, 성능 저하 유발 코드 점검\n"
        "3. 개선 제안: 더 깔끔하고 효율적인 리팩토링 방향 제안\n\n"
        "친절하지만 전문적인 어조로 작성해줘. 개발자에게 도움이 되는 피드백 위주로 구성해줘.\n\n"
        f"--- 코드 변경 사항 ---\n{pr_diff}"
    )

    payload = {
        "contents": [
            {
                "parts": [
                    {"text": prompt}
                ]
            }
        ]
    }

    print("Gemini AI 분석 요청 중...")
    gemini_response = requests.post(gemini_url, json=payload)
    
    if gemini_response.status_code != 200:
        print("Gemini API 호출에 실패했습니다:", gemini_response.text)
        sys.exit(1)

    try:
        ai_review = gemini_response.json()['candidates'][0]['content']['parts'][0]['text']
    except (KeyError, IndexError):
        print("Gemini 응답 파싱 실패")
        sys.exit(1)

    # 3. 분석 결과를 GitHub PR 댓글로 등록하기
    comment_url = f"https://api.github.com/repos/{repo}/issues/{pr_number}/comments"
    comment_headers = {
        "Authorization": f"token {github_token}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    comment_body = {
        "body": f"🤖 **Gemini AI 실시간 코드 리뷰 리포트**\n\n{ai_review}"
    }

    print("GitHub PR에 댓글 작성 중...")
    comment_response = requests.post(comment_url, headers=comment_headers, json=comment_body)

    if comment_response.status_code == 201:
        print("AI 리뷰가 성공적으로 작성되었습니다!")
    else:
        print("GitHub 댓글 작성 실패:", comment_response.text)

if __name__ == "__main__":
    main()
