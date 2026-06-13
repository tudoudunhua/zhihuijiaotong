<template>
  <div class="user-page">
    <el-card>
      <template #header>
        <div class="head">
          <span>用户管理与查看权限</span>
          <el-button type="primary" size="small" @click="openCreate">新增用户</el-button>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column label="可查看模块">
          <template #default="{ row }">
            <el-tag
              v-for="p in (row.menuPermissions || [])"
              :key="`${row.userId}-${p}`"
              size="small"
              style="margin-right: 6px; margin-bottom: 6px;"
            >
              {{ permissionLabel(p) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑权限</el-button>
            <el-popconfirm
              title="确认删除该用户吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="removeUser(row)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除用户</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="编辑用户权限" width="560px">
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="editing.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editing.role" style="width: 100%" @change="onEditingRoleChange">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="交警处置员" value="OFFICER" />
            <el-option label="数据分析员" value="ANALYST" />
            <el-option label="路段网格员" value="ROAD_GRID" />
            <el-option label="网格员" value="GRID_MEMBER" />
            <el-option label="交警中队" value="TRAFFIC_SQUAD" />
            <el-option label="消防" value="FIRE" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="查看权限">
          <el-checkbox-group v-model="editing.menuPermissions">
            <el-checkbox v-for="opt in permissionOptions" :key="opt.value" :label="opt.value">
              {{ opt.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createDialogVisible" title="新增用户" width="560px">
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="creating.username" placeholder="请输入登录用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="creating.password" placeholder="请输入登录密码" show-password />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="creating.realName" placeholder="请输入姓名（可选）" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="creating.role" style="width: 100%" @change="onCreatingRoleChange">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="交警处置员" value="OFFICER" />
            <el-option label="数据分析员" value="ANALYST" />
            <el-option label="路段网格员" value="ROAD_GRID" />
            <el-option label="网格员" value="GRID_MEMBER" />
            <el-option label="交警中队" value="TRAFFIC_SQUAD" />
            <el-option label="消防" value="FIRE" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="查看权限">
          <el-checkbox-group v-model="creating.menuPermissions">
            <el-checkbox v-for="opt in permissionOptions" :key="`create-${opt.value}`" :label="opt.value">
              {{ opt.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingLoading" @click="saveCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getUsers, updateUserPermissions, createUser, deleteUser } from '@/api/user'

const OPTIONS = [
  { value: 'view_home', label: '首页' },
  { value: 'view_violations', label: '违章记录' },
  { value: 'view_pending', label: '待处理违章' },
  { value: 'view_add_violation', label: '添加记录' },
  { value: 'view_statistics', label: '统计分析' },
  { value: 'view_intelligent', label: '智能预警' },
  { value: 'view_warnings', label: '预警列表' },
  { value: 'view_cameras', label: '摄像头管理' },
  { value: 'view_user_manage', label: '用户管理' },
  { value: 'view_operation_logs', label: '操作日志' },
]

export default {
  name: 'UserManagement',
  data() {
    return {
      loading: false,
      saving: false,
      users: [],
      dialogVisible: false,
      createDialogVisible: false,
      creatingLoading: false,
      editing: {
        userId: null,
        username: '',
        role: 'USER',
        menuPermissions: [],
      },
      creating: {
        username: '',
        password: '',
        realName: '',
        role: 'USER',
        menuPermissions: ['view_home', 'view_violations'],
      },
      permissionOptions: OPTIONS,
    }
  },
  mounted() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      this.loading = true
      try {
        const res = await getUsers()
        this.users = res.data || []
      } catch (e) {
        this.$message.error('加载用户列表失败')
      } finally {
        this.loading = false
      }
    },
    openEdit(row) {
      this.editing = {
        userId: row.userId,
        username: row.username,
        role: (row.role || 'USER').toUpperCase(),
        menuPermissions: [...(row.menuPermissions || [])],
      }
      this.dialogVisible = true
    },
    openCreate() {
      this.creating = {
        username: '',
        password: '',
        realName: '',
        role: 'USER',
        menuPermissions: this.defaultPermissionsByRole('USER'),
      }
      this.createDialogVisible = true
    },
    onCreatingRoleChange(role) {
      this.creating.menuPermissions = this.defaultPermissionsByRole(role)
    },
    async savePermissions() {
      if (this.saving) return
      if (!this.editing.userId) {
        this.$message.warning('当前编辑用户无效，请重新打开弹窗')
        return
      }
      if (!Array.isArray(this.editing.menuPermissions) || this.editing.menuPermissions.length === 0) {
        this.$message.warning('请至少勾选一个查看权限')
        return
      }
      this.saving = true
      try {
        await updateUserPermissions(this.editing.userId, {
          role: this.editing.role,
          menuPermissions: this.editing.menuPermissions,
        })
        this.$message.success(`保存成功（用户「${this.editing.username}」重新登录后生效）`)
        this.dialogVisible = false
        await this.loadUsers()
      } catch (e) {
        const msg = e?.response?.data || '保存失败'
        this.$message.error(String(msg))
      } finally {
        this.saving = false
      }
    },
    async saveCreate() {
      if (this.creatingLoading) return
      if (!this.creating.username || !this.creating.password) {
        this.$message.warning('用户名和密码不能为空')
        return
      }
      if (!Array.isArray(this.creating.menuPermissions) || this.creating.menuPermissions.length === 0) {
        this.$message.warning('请至少勾选一个查看权限')
        return
      }
      this.creatingLoading = true
      try {
        await createUser({
          username: this.creating.username,
          password: this.creating.password,
          realName: this.creating.realName,
          role: this.creating.role,
          menuPermissions: this.creating.menuPermissions,
        })
        this.$message.success('创建成功')
        this.createDialogVisible = false
        await this.loadUsers()
      } catch (e) {
        const msg = e?.response?.data || '创建失败'
        this.$message.error(String(msg))
      } finally {
        this.creatingLoading = false
      }
    },
    async removeUser(row) {
      try {
        await deleteUser(row.userId)
        this.$message.success('删除成功')
        await this.loadUsers()
      } catch (e) {
        const msg = e?.response?.data || '删除失败'
        this.$message.error(String(msg))
      }
    },
    onEditingRoleChange(role) {
      // 编辑场景下切角色时，自动带出该角色常用权限，减少“改了没生效”的误感知
      this.editing.menuPermissions = this.defaultPermissionsByRole(role)
    },
    defaultPermissionsByRole(role) {
      const r = String(role || 'USER').toUpperCase()
      if (r === 'ADMIN') {
        return ['view_home', 'view_violations', 'view_pending', 'view_add_violation', 'view_statistics', 'view_intelligent', 'view_warnings', 'view_cameras', 'view_user_manage', 'view_operation_logs']
      }
      if (r === 'OFFICER') {
        return ['view_home', 'view_violations', 'view_pending', 'view_add_violation', 'view_warnings', 'view_cameras']
      }
      if (r === 'ANALYST') {
        return ['view_home', 'view_statistics', 'view_intelligent', 'view_warnings']
      }
      if (['ROAD_GRID', 'GRID_MEMBER', 'TRAFFIC_SQUAD', 'FIRE'].includes(r)) {
        return ['view_home', 'view_warnings']
      }
      return ['view_home', 'view_violations']
    },
    permissionLabel(value) {
      const hit = this.permissionOptions.find(i => i.value === value)
      return hit ? hit.label : value
    },
  },
}
</script>

<style scoped>
.user-page {
  padding: 8px;
}
.head {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
