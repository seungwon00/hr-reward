// 인증 API 호출 함수. 백엔드 API가 준비되면 console.log 대신 fetch로 교체한다.

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface SignUpRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
}

export async function login(request: LoginRequest): Promise<void> {
  // TODO: POST /api/v1/auth/login
  // const res = await fetch('/api/v1/auth/login', {
  //   method: 'POST',
  //   headers: { 'Content-Type': 'application/json' },
  //   body: JSON.stringify(request),
  // });
  console.log('[api] POST /api/v1/auth/login', { ...request, password: '***' });
}

export async function signUp(request: SignUpRequest): Promise<void> {
  // TODO: 회원가입 API 엔드포인트 확정 후 연결
  console.log('[api] signUp', { ...request, password: '***' });
}
