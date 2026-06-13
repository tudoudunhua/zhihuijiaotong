<template>
  <div class="form-container">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><VideoCamera /></el-icon>
            {{ isEdit ? '编辑摄像头设备' : '新增摄像头设备' }}
          </span>
          <el-button link class="back-button" @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="camera"
        :rules="rules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="摄像头名称" prop="cameraName">
          <el-input
            v-model="camera.cameraName"
            placeholder="例如：交通监控-紫荆山路口、金水东路与心怡路交叉口"
            clearable
          />
        </el-form-item>

        <el-form-item label="安装位置" prop="location">
          <el-select
            v-model="camera.location"
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入郑州市街道/路口"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="loc in zhengzhouLocations"
              :key="loc"
              :label="loc"
              :value="loc"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="IP 地址" prop="ipAddress">
          <el-input
            v-model="camera.ipAddress"
            placeholder="例如：192.168.1.100，可选"
            clearable
          />
        </el-form-item>

        <el-form-item label="设备状态" prop="status">
          <el-select v-model="camera.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="在线" value="ONLINE" />
            <el-option label="离线" value="OFFLINE" />
            <el-option label="故障" value="FAULT" />
          </el-select>
        </el-form-item>

        <el-form-item label="安装时间" prop="installTime">
          <el-date-picker
            v-model="camera.installTime"
            type="datetime"
            placeholder="选择安装时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注说明" prop="remark">
          <el-input
            v-model="camera.remark"
            type="textarea"
            :rows="3"
            placeholder="可填写覆盖范围、方向、补光等信息"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitCamera">
            <el-icon><Check /></el-icon>
            {{ isEdit ? '更新设备' : '保存设备' }}
          </el-button>
          <el-button @click="resetForm">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { VideoCamera, ArrowLeft, Check, Refresh } from '@element-plus/icons-vue'
import { getCameraById, addCamera, updateCamera } from '@/api/camera'

// 郑州市主要街道、路口、区域（安装位置候选）
const ZHENGZHOU_LOCATIONS = [
  '金水区-经三路', '金水区-花园路', '金水区-紫荆山路', '金水区-文化路', '金水区-东风路', '金水区-农业路', '金水区-黄河路', '金水区-金水路',
  '中原区-建设路', '中原区-嵩山路', '中原区-桐柏路', '中原区-中原路', '中原区-陇海路',
  '二七区-大学路', '二七区-京广路', '二七区-航海路', '二七区-长江路', '二七区-二七广场',
  '管城区-城东路', '管城区-紫荆山路', '管城区-东大街', '管城区-南大街',
  '惠济区-江山路', '惠济区-天河路', '惠济区-开元路', '惠济区-北三环',
  '郑东新区-金水东路', '郑东新区-心怡路', '郑东新区-商都路', '郑东新区-东风东路', '郑东新区-CBD 商务外环',
  '高新区-科学大道', '高新区-瑞达路', '高新区-长椿路', '高新区-雪松路',
  '经开区-航海东路', '经开区-经北二路', '经开区-第八大街', '经开区-朝凤路',
  '紫荆山路口', '大石桥', '新通桥', '绿城广场', '郑州火车站东广场', '郑州东站', '人民路与太康路', '花园路与农业路', '经三路与农科路', '大学路与陇海路', '中原路与嵩山路', '南三环与大学路', '北三环与花园路', '西三环与中原路', '东三环与金水东路',
]

export default {
  name: 'CameraForm',
  components: {
    VideoCamera,
    ArrowLeft,
    Check,
    Refresh,
  },
  data() {
    return {
      submitting: false,
      isEdit: false,
      zhengzhouLocations: ZHENGZHOU_LOCATIONS,
      camera: {
        cameraName: '',
        location: '',
        ipAddress: '',
        status: 'ONLINE',
        installTime: '',
        remark: '',
      },
      rules: {
        cameraName: [{ required: true, message: '请输入摄像头名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
      },
    }
  },
  mounted() {
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.fetchCamera(id)
    } else {
      // 默认安装时间为当前
      this.camera.installTime = this.formatDateTime(new Date())
    }
  },
  methods: {
    formatDateTime(date) {
      if (!date) return ''
      const d = typeof date === 'string' ? new Date(date) : date
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
        d.getHours(),
      )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },

    toBackendDate(dateTimeStr) {
      // 后端 ObjectMapper 期望格式: yyyy-MM-dd HH:mm:ss
      if (!dateTimeStr) return null
      const s = String(dateTimeStr).trim()
      return s || null
    },

    async fetchCamera(id) {
      try {
        const res = await getCameraById(id)
        const data = res.data
        this.camera = {
          cameraName: data.cameraName,
          location: data.location,
          ipAddress: data.ipAddress,
          status: data.status || 'ONLINE',
          installTime: data.installTime
            ? this.formatDateTime(data.installTime)
            : '',
          remark: data.remark || '',
        }
      } catch (e) {
        this.$message.error('获取摄像头信息失败')
        this.goBack()
      }
    },

    submitCamera() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          const cameraName = (this.camera.cameraName || '').trim()
          const payload = {
            ...this.camera,
            cameraName,
            installTime: this.toBackendDate(this.camera.installTime),
          }
          if (this.isEdit) {
            await updateCamera(this.$route.params.id, payload)
            this.$message.success('更新成功')
          } else {
            await addCamera(payload)
            this.$message.success('保存成功')
          }
          this.$router.push('/cameras')
        } catch (e) {
          let msg = this.isEdit ? '更新失败' : '保存失败'
          const data = e?.response?.data
          if (typeof data === 'string') msg = data
          else if (data?.message) msg = data.message
          else if (e?.message) msg = e.message
          this.$message.error(msg)
          console.error(e)
        } finally {
          this.submitting = false
        }
      })
    },

    resetForm() {
      this.$refs.formRef.resetFields()
      if (!this.isEdit) {
        this.camera.installTime = this.formatDateTime(new Date())
      }
    },

    goBack() {
      this.$router.push('/cameras')
    },
  },
}
</script>

<style scoped>
.form-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 30px 20px;
}

.form-card {
  max-width: 800px;
  margin: 0 auto;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 20px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-button {
  color: #606266;
}

.back-button:hover {
  color: #42a5f5;
}

::deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

::deep(.el-input__inner),
::deep(.el-select .el-input__inner) {
  border-radius: 8px;
}

::deep(.el-button) {
  border-radius: 8px;
}
</style>


