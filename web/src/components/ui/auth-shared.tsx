import React, { useState } from 'react';
import { Clock, Eye, EyeOff, Gift, Ticket, type LucideIcon } from 'lucide-react';

// 로그인·회원가입 페이지가 함께 쓰는 입력 래퍼와 오른쪽 소개 영역

export const GlassInputWrapper = ({ children }: { children: React.ReactNode }) => (
  <div className="rounded-2xl border border-border bg-foreground/5 backdrop-blur-sm transition-colors focus-within:border-violet-400/70 focus-within:bg-violet-500/10">
    {children}
  </div>
);

interface FieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
}

export const Field = ({ label, id, className, ...inputProps }: FieldProps) => (
  <div className={className}>
    <label htmlFor={id} className="text-sm font-medium text-muted-foreground">{label}</label>
    <GlassInputWrapper>
      <input id={id} className="w-full bg-transparent text-sm p-4 rounded-2xl focus:outline-none" {...inputProps} />
    </GlassInputWrapper>
  </div>
);

export const PasswordField = ({ label, id, className, ...inputProps }: FieldProps) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className={className}>
      <label htmlFor={id} className="text-sm font-medium text-muted-foreground">{label}</label>
      <GlassInputWrapper>
        <div className="relative">
          <input
            id={id}
            type={showPassword ? 'text' : 'password'}
            className="w-full bg-transparent text-sm p-4 pr-12 rounded-2xl focus:outline-none"
            {...inputProps}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
            className="absolute inset-y-0 right-3 flex items-center"
          >
            {showPassword
              ? <EyeOff className="w-5 h-5 text-muted-foreground hover:text-foreground transition-colors" />
              : <Eye className="w-5 h-5 text-muted-foreground hover:text-foreground transition-colors" />}
          </button>
        </div>
      </GlassInputWrapper>
    </div>
  );
};

interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

const FEATURES: Feature[] = [
  { icon: Clock, title: '간편한 출퇴근 기록', description: '버튼 한 번으로 출근·퇴근을 기록하고 근무 시간을 자동으로 계산해요.' },
  { icon: Gift, title: '근태 기반 자동 보상', description: '지각 없이 꾸준히 근무하면 정해 둔 규칙에 따라 보상이 쌓여요.' },
  { icon: Ticket, title: '쿠폰으로 직원 동기부여', description: '사장님이 발급한 쿠폰으로 직원의 성실함에 바로 보답할 수 있어요.' },
];

const FEATURE_DELAYS = ['animate-delay-1000', 'animate-delay-1200', 'animate-delay-1400'];

const FeatureCard = ({ feature, delay }: { feature: Feature; delay: string }) => {
  const Icon = feature.icon;
  return (
    <div className={`animate-feature ${delay} flex items-start gap-4 rounded-3xl bg-white/15 backdrop-blur-xl border border-white/20 p-5 text-white`}>
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-white/20">
        <Icon className="h-5 w-5" />
      </div>
      <div className="text-sm leading-snug">
        <p className="font-semibold">{feature.title}</p>
        <p className="mt-1 text-white/80">{feature.description}</p>
      </div>
    </div>
  );
};

// 외부 이미지 대신 그라데이션 배경 사용
export const AuthHero = () => (
  <section className="hidden md:block flex-1 relative p-4">
    <div className="animate-slide-right animate-delay-300 absolute inset-4 overflow-hidden rounded-3xl bg-gradient-to-br from-violet-600 via-indigo-500 to-sky-400">
      <div className="absolute -top-24 -right-24 h-80 w-80 rounded-full bg-fuchsia-400/40 blur-3xl" />
      <div className="absolute -bottom-32 -left-16 h-96 w-96 rounded-full bg-sky-300/40 blur-3xl" />
    </div>
    <div className="relative flex h-full flex-col justify-end gap-4 p-10 lg:p-14">
      <h2 className="animate-element animate-delay-700 text-3xl lg:text-4xl font-semibold leading-tight text-white">
        일한 만큼,<br />제대로 보상받는 매장
      </h2>
      <p className="animate-element animate-delay-800 mb-4 text-white/80">
        출퇴근 기록부터 직원 보상까지 한곳에서 관리하세요.
      </p>
      <div className="flex flex-col gap-3 max-w-md">
        {FEATURES.map((feature, i) => (
          <FeatureCard key={feature.title} feature={feature} delay={FEATURE_DELAYS[i]} />
        ))}
      </div>
    </div>
  </section>
);
