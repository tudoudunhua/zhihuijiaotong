<template>
  <router-link to="/warnings">预警管理</router-link>
  <router-link to="/intelligent" style="margin-left:12px">智能预警</router-link>

  <div class="header">
    <div class="left">
      🚗 智慧交通违章管理系统
    </div>

    <div class="right">
      <span v-if="user">
        欢迎，{{ user.realName || user.username }}
      </span>
      <span class="logout" @click="logout">退出</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AppHeader',
  data() {
    return {
      user: null
    }
  },
  mounted() {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      this.user = JSON.parse(userStr)
    }
  },
  methods: {
    logout() {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('user')
    this.user = null
    this.$router.push('/login')
  }

  }
}
</script>

<style scoped>
.header {
  height: 60px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.right span {
  margin-left: 15px;
  cursor: pointer;
}

.logout:hover {
  text-decoration: underline;
}
</style>
