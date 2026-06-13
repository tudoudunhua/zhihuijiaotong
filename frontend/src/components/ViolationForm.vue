<template>
  <div class="form-container">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Edit /></el-icon>
            {{ isEdit ? '编辑违章记录' : '添加违章记录' }}
          </span>
          <el-button 
            link
            @click="goBack"
            class="back-button"
          >
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <el-form 
        ref="formRef"
        :model="violation" 
        :rules="rules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input 
            v-model="violation.plateNumber" 
            placeholder="请输入车牌号（如：京A12345）"
            clearable
            maxlength="20"
          >
            <template #prefix>
              <el-icon><Van /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="违章类型" prop="violationType">
          <el-select 
            v-model="violation.violationType" 
            placeholder="请选择违章类型"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="type in violationTypes"
              :key="type"
              :label="type"
              :value="type"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="违章时间" prop="violationTime">
          <el-date-picker
            v-model="violation.violationTime"
            type="datetime"
            placeholder="选择违章时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="罚款金额" prop="fineAmount">
          <el-input-number
            v-model="violation.fineAmount"
            :min="0"
            :max="10000"
            :precision="2"
            placeholder="请输入罚款金额"
            style="width: 100%"
          >
            <template #prefix>¥</template>
          </el-input-number>
        </el-form-item>

        <el-form-item label="扣分" prop="pointsDeducted">
          <el-input-number
            v-model="violation.pointsDeducted"
            :min="0"
            :max="12"
            :precision="0"
            placeholder="请输入扣分数"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="违章位置" prop="violationLocation">
          <el-input
            v-model="violation.violationLocation"
            placeholder="请输入违章位置（经纬度）"
            clearable
          />
        </el-form-item>

        <el-form-item label="违章照片">
          <div v-if="isEdit" class="image-upload-wrapper">
            <el-image
              v-if="violation.violationImg"
              :src="fullImageUrl(violation.violationImg)"
              :preview-src-list="[fullImageUrl(violation.violationImg)]"
              fit="cover"
              class="violation-image"
            />
            <div class="upload-actions">
              <el-upload
                class="upload-btn"
                :show-file-list="false"
                :action="uploadUrl"
                name="file"
                accept="image/*"
                :before-upload="beforeUpload"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
              >
                <el-button type="primary">
                  {{ violation.violationImg ? '更换照片' : '上传照片' }}
                </el-button>
              </el-upload>
              <el-button type="success" plain @click="generateDemoImage">
                自动生成示例照片
              </el-button>
            </div>
            <div class="image-tip">支持 JPG/PNG，大小不超过 5MB，仅用于违章取证展示。示例照片仅用于演示。</div>
          </div>
          <div v-else class="image-tip">
            请先保存基础信息，保存成功后即可上传或自动生成违章照片。
            <div style="margin-top: 10px;">
              <el-button type="primary" plain @click="saveAndEnableImageUpload" :loading="submitting">
                先保存并进入照片模式
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="抓拍路口/设备" prop="deviceId">
          <el-select
            v-model="selectedCameraId"
            placeholder="请选择摄像头（区/路段）"
            filterable
            clearable
            style="width: 100%"
            @change="onCameraChange"
          >
            <el-option
              v-for="camera in cameras"
              :key="camera.cameraId"
              :label="cameraOptionLabel(camera)"
              :value="Number(camera.cameraId)"
            />
          </el-select>
          <div class="image-tip" style="margin-top: 6px;">
            已关联设备ID：{{ violation.deviceId || '-' }}（选摄像头后自动回填）
          </div>
        </el-form-item>

        <el-form-item label="预警等级" prop="warningLevel">
          <el-select v-model="violation.warningLevel" placeholder="选择预警等级" style="width: 100%">
            <el-option label="BLUE" value="BLUE" />
            <el-option label="YELLOW" value="YELLOW" />
            <el-option label="ORANGE" value="ORANGE" />
            <el-option label="RED" value="RED" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="submitViolation"
            :loading="submitting"
            size="large"
          >
            <el-icon><Check /></el-icon>
            {{ isEdit ? '更新记录' : '保存基础信息' }}
          </el-button>
          <el-button 
            @click="resetForm"
            size="large"
          >
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

  </div>
</template>

<script>
  import { Edit, ArrowLeft, Van, Check, Refresh } from '@element-plus/icons-vue'
  import {
    getViolationById,
    addViolation,
    updateViolation,
    autoGenerateViolationImage,
  } from '@/api/violation'
  import { getCameras } from '@/api/camera'
  
  export default {
    name: 'ViolationForm',
    components: {
      Edit,
      ArrowLeft,
      Van,
      Check,
      Refresh,
    },
    data() {
      return {
        submitting: false,
        isEdit: false,
        uploadUrl: '',
        cameras: [],
        selectedCameraId: null,
        violation: {
          plateNumber: '',
          violationType: '',
          violationTime: '',
          fineAmount: null,
          pointsDeducted: null,
          violationLocation: '',
          deviceId: null,
          roadId: null,
          warningLevel: 'BLUE',
          violationImg: '',
        },
        violationTypes: [
          '超速行驶',
          '闯红灯',
          '违法停车',
          '未系安全带',
          '酒驾',
          '无证驾驶',
          '逆行',
          '违法变道',
          '占用应急车道',
          '其他',
        ],
        rules: {
          plateNumber: [
            { required: true, message: '请输入车牌号', trigger: 'blur' },
          ],
          violationType: [
            { required: true, message: '请选择违章类型', trigger: 'change' },
          ],
          violationTime: [
            { required: true, message: '请选择违章时间', trigger: 'change' },
          ],
          fineAmount: [
            { required: true, message: '请输入罚款金额', trigger: 'blur' },
          ],
          pointsDeducted: [
            { required: true, message: '请输入扣分', trigger: 'blur' },
          ],
        },
      }
    },
    mounted() {
      this.loadCameras()
      const id = this.$route.params.id
      if (id) {
        this.isEdit = true
        this.fetchViolation(id)
        this.uploadUrl = `http://localhost:8081/api/violations/${id}/image`
      } else {
        this.violation.violationTime = this.formatDateTime(new Date())
      }
    },
    methods: {
      async loadCameras() {
        try {
          const res = await getCameras()
          this.cameras = res.data || []
        } catch (e) {
          console.error('加载摄像头失败', e)
          this.cameras = []
        }
      },

      cameraOptionLabel(camera) {
        const loc = camera?.location ? `（${camera.location}）` : ''
        return `${camera?.cameraName || '未命名摄像头'}${loc}`
      },

      onCameraChange(cameraId) {
        if (!cameraId) return
        const camera = (this.cameras || []).find(c => Number(c.cameraId) === Number(cameraId))
        if (!camera) return

        // 关联抓拍设备
        this.violation.deviceId = Number(camera.cameraId)

        // 位置为空时自动回填摄像头安装位置
        if (!this.violation.violationLocation && camera.location) {
          this.violation.violationLocation = camera.location
        }
      },

      formatDateTime(date) {
        const y = date.getFullYear()
        const m = String(date.getMonth() + 1).padStart(2, '0')
        const d = String(date.getDate()).padStart(2, '0')
        const h = String(date.getHours()).padStart(2, '0')
        const mm = String(date.getMinutes()).padStart(2, '0')
        const s = String(date.getSeconds()).padStart(2, '0')
        return `${y}-${m}-${d} ${h}:${mm}:${s}`
      },
  
      /** ✅ 获取单条数据 */
      async fetchViolation(id) {
        try {
          const res = await getViolationById(id)
          const data = res.data
          this.violation = {
            plateNumber: data.plateNumber,
            violationType: data.violationType,
            violationTime: data.violationTime
              ? data.violationTime.replace('T', ' ').substring(0, 19)
              : '',
            fineAmount: data.fineAmount,
            pointsDeducted: data.pointsDeducted,
            violationLocation: data.violationLocation,
            deviceId: data.deviceId,
            roadId: data.roadId,
            warningLevel: data.warningLevel || 'BLUE',
            violationImg: data.violationImg || '',
          }
          if (this.violation.deviceId) {
            this.selectedCameraId = Number(this.violation.deviceId)
          }
        } catch (e) {
          this.$message.error('获取违章信息失败')
          this.goBack()
        }
      },
  
      /** ✅ 提交（新增 / 编辑） */
      submitViolation() {
        this.persistViolation({ switchToEditAfterCreate: true, navigateToListAfterUpdate: true })
      },

      // 在新增页先保存基础信息，保存后留在当前页继续上传/生成照片
      saveAndEnableImageUpload() {
        this.persistViolation({ switchToEditAfterCreate: true, navigateToListAfterUpdate: false })
      },

      persistViolation({ switchToEditAfterCreate = true, navigateToListAfterUpdate = true } = {}) {
        this.$refs.formRef.validate(async valid => {
          if (!valid) return
  
          this.submitting = true
          try {
            const payload = {
              ...this.violation,
              violationTime: new Date(this.violation.violationTime).toISOString(),
            }
  
            if (this.isEdit) {
              await updateViolation(this.$route.params.id, payload)
              this.$message.success('更新成功')
              if (navigateToListAfterUpdate) {
                this.$router.push('/violations')
              }
            } else {
              const res = await addViolation(payload)
              const saved = res?.data || {}
              const id = saved.violationId
              this.$message.success('基础信息已保存')

              if (switchToEditAfterCreate && id) {
                this.isEdit = true
                this.uploadUrl = `http://localhost:8081/api/violations/${id}/image`
                this.violation.violationImg = saved.violationImg || this.violation.violationImg
                this.$router.replace(`/add-violation/${id}`)
              }
            }
          } catch (e) {
            this.$message.error(this.isEdit ? '更新失败' : '提交失败')
            console.error(e)
          } finally {
            this.submitting = false
          }
        })
      },
  
      resetForm() {
        this.$refs.formRef.resetFields()
        if (!this.isEdit) {
          this.violation.violationTime = this.formatDateTime(new Date())
        }
      },
  
      goBack() {
        this.$router.push('/violations')
      },

      // 拼接完整图片 URL
      fullImageUrl(path) {
        if (!path) return ''
        if (path.startsWith('http://') || path.startsWith('https://')) {
          return path
        }
        const backend = 'http://localhost:8081'
        return backend + (path.startsWith('/') ? path : `/${path}`)
      },

      // 上传前校验（前端再限制一次大小）
      beforeUpload(file) {
        const isLt5M = file.size / 1024 / 1024 < 5
        if (!isLt5M) {
          this.$message.error('图片大小不能超过 5MB')
        }
        return isLt5M
      },

      handleUploadSuccess(response) {
        const url = response?.violationImg || response?.data?.violationImg
        if (url) {
          this.violation.violationImg = url
        }
        this.$message.success('违章照片上传成功')
      },

      handleUploadError() {
        this.$message.error('违章照片上传失败')
      },

      async generateDemoImage() {
        try {
          const id = this.$route.params.id
          if (!id) {
            this.$message.warning('请先保存违章记录，再生成示例照片')
            return
          }
          const res = await autoGenerateViolationImage(id)
          const url = res?.violationImg || res?.data?.violationImg
          if (url) {
            this.violation.violationImg = url
          }
          this.$message.success('已根据违章类型自动匹配示例照片')
        } catch (e) {
          console.error(e)
          this.$message.error('生成示例照片失败')
        }
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


