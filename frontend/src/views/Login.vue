<template>
  <div class="login-page">
    <!-- 科技感隧道背景（纯 CSS） -->
    <div class="tech-bg" aria-hidden="true">
      <div class="tech-vignette"></div>
      <div class="tech-tunnel"></div>
      <div class="tech-grid"></div>
      <div class="tech-particles"></div>
    </div>
    <el-card class="login-card">
      <h2 class="title">🚦 智慧交通违章管理系统</h2>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="0"
        @keydown.enter.prevent="submit"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            clearable
            @keydown.enter.prevent="submit"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            clearable
            @keydown.enter.prevent="submit"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="demo-users">
        <div class="demo-title">账号：</div>
        <div class="demo-item">管理员：`admin / 123456`</div>
        <div class="demo-item">交警处置员：`officer / 123456`</div>
        <div class="demo-item">数据分析员：`analyst / 123456`</div>
        <div class="demo-item">路段网格员：`roadgrid / 123456`</div>
        <div class="demo-item">网格员：`grid / 123456`</div>
        <div class="demo-item">交警中队：`squad / 123456`</div>
        <div class="demo-item">消防：`fire / 123456`</div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { login } from '@/api/auth'

export default {
  name: 'LoginView',

  data() {
    return {
      loading: false,
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      }
    }
  },

  methods: {
    submit() {
      if (this.loading) return
      this.$refs.formRef.validate(async (valid) => {
        if (!valid) return

        this.loading = true
        try {
          const res = await login(this.form)
          const data = res.data

          // 根据你后端返回结构
          const token = data.token || data.data?.token
          const user = data.user || data.data?.user

          if (!token) {
            this.$message.error('登录失败：未返回 token')
            return
          }

          // 保存 token & 用户
          localStorage.setItem('jwt_token', token)
          localStorage.setItem('user', JSON.stringify(user || {}))
          window.dispatchEvent(new Event('auth-changed'))

          this.$message.success('登录成功')
          this.$router.push('/')

        } catch (e) {
          console.error(e)
          this.$message.error('登录失败：用户名或密码错误')
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  background: #050b1a;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.tech-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}

/* 去掉“中间强光”，改成暗角与远处氛围 */
.tech-vignette {
  position: absolute;
  inset: -20%;
  background:
    /* 提亮整体氛围（不再有中心强光） */
    radial-gradient(1100px 680px at 50% 58%, rgba(56, 189, 248, 0.16), rgba(56, 189, 248, 0) 62%),
    radial-gradient(1200px 820px at 50% 60%, rgba(2, 6, 23, 0) 28%, rgba(2, 6, 23, 0.78) 78%);
  opacity: 1;
}

/* 隧道线框：横竖细线 + 透视裁剪 */
.tech-tunnel {
  position: absolute;
  inset: -35%;
  background-image:
    repeating-linear-gradient(90deg, rgba(59, 130, 246, 0.24) 0 1px, transparent 1px 90px),
    repeating-linear-gradient(rgba(59, 130, 246, 0.18) 0 1px, transparent 1px 70px);
  mix-blend-mode: screen;
  opacity: 0.50;
  transform: perspective(900px) rotateX(55deg) rotateZ(-10deg) scale(1.35) translateY(10%);
  transform-origin: 50% 60%;
  mask-image: radial-gradient(closest-side at 50% 55%, rgba(0,0,0,1), rgba(0,0,0,0) 75%);
  animation: tunnelMove 10s linear infinite;
}

/* 地面网格：更强透视感 */
.tech-grid {
  position: absolute;
  left: -40%;
  right: -40%;
  top: 45%;
  bottom: -60%;
  background-image:
    /* 网状/菱形网格：横竖 + 两条斜线 */
    repeating-linear-gradient(90deg, rgba(56, 189, 248, 0.18) 0 1px, transparent 1px 62px),
    repeating-linear-gradient(rgba(56, 189, 248, 0.16) 0 1px, transparent 1px 44px),
    repeating-linear-gradient(45deg, rgba(56, 189, 248, 0.14) 0 1px, transparent 1px 54px),
    repeating-linear-gradient(-45deg, rgba(56, 189, 248, 0.14) 0 1px, transparent 1px 54px);
  opacity: 0.48;
  transform: perspective(760px) rotateX(70deg) translateY(0);
  transform-origin: 50% 0%;
  filter: drop-shadow(0 0 6px rgba(56, 189, 248, 0.18));
  animation: gridFlow 2.6s linear infinite;
}

/* 粒子：点阵 + 漂移（你要的“后面粒子稍微动态”） */
.tech-particles {
  position: absolute;
  inset: -30%;
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.20) 1px, transparent 1px),
    radial-gradient(circle, rgba(56, 189, 248, 0.22) 1px, transparent 1px),
    radial-gradient(circle, rgba(37, 99, 235, 0.18) 1px, transparent 1px);
  background-size: 26px 26px, 42px 42px, 58px 58px;
  background-position: 0 0, 18px 8px, 30px 24px;
  opacity: 0.42;
  transform: translate3d(0, 0, 0);
  animation: particlesDrift 14s linear infinite;
}

.login-card {
  width: 420px;
  padding: 30px;
  border-radius: 16px;
  position: relative;
  z-index: 2;
  /* 与背景融合：更偏蓝、半透明、阴影更轻 */
  background: rgba(10, 24, 52, 0.26);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(56, 189, 248, 0.26);
  box-shadow:
    0 18px 54px rgba(2, 6, 23, 0.58),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.title {
  text-align: center;
  margin-bottom: 20px;
  color: rgba(255, 255, 255, 0.96);
  font-weight: 900;
  letter-spacing: 0.6px;
  text-shadow: 0 0 18px rgba(56, 189, 248, 0.22);
}

.demo-users {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px dashed rgba(56, 189, 248, 0.25);
}

.demo-title {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  margin-bottom: 4px;
}

.demo-item {
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
  line-height: 1.5;
}

/* 让输入框也更融入玻璃卡片 */
::deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.10) !important;
  border: 1px solid rgba(56, 189, 248, 0.16) !important;
  box-shadow: none !important;
}
::deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.92);
}
::deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.55);
}
::deep(.el-form-item) {
  margin-bottom: 14px;
}

/* 登录按钮更突出（渐变+轻微光晕） */
::deep(.el-button--primary) {
  border: none;
  background: linear-gradient(90deg, #38bdf8 0%, #2563eb 55%, #7c3aed 110%) !important;
  box-shadow: 0 10px 26px rgba(37, 99, 235, 0.35);
}
::deep(.el-button--primary:hover) {
  filter: brightness(1.06);
}

@keyframes tunnelMove {
  0% { background-position: 0 0, 0 0; }
  100% { background-position: 180px 0, 0 140px; }
}

@keyframes gridFlow {
  0% { background-position: 0 0, 0 0; }
  100% { background-position: 0 120px, 0 84px; }
}

@keyframes particlesDrift {
  0% { background-position: 0 0, 18px 8px, 30px 24px; transform: translate3d(0,0,0); }
  100% { background-position: -260px 180px, -300px 210px, -340px 240px; transform: translate3d(-14px, 10px, 0); }
}

@media (prefers-reduced-motion: reduce) {
  .login-page *,
  .tech-bg * {
    animation: none !important;
  }
}
</style>
