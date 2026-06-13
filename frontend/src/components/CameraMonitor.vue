<template>
  <el-dialog
    v-model="visible"
    :title="`实时监控 - ${camera?.cameraName || ''}`"
    width="90%"
    top="5vh"
    destroy-on-close
    class="camera-monitor-dialog"
    @close="onClose"
  >
    <div class="monitor-container">
      <!-- 模拟监控画面 -->
      <div class="monitor-frame">
        <div class="video-area">
          <!-- 使用不同 seed 模拟不同摄像头画面，定期切换增加“实时”感 -->
          <img
            v-show="!imgError"
            :src="feedImageUrl"
            alt="监控画面"
            class="feed-image"
            @error="imgError = true"
          />
          <div v-show="imgError" class="feed-placeholder">
            <span class="placeholder-text">模拟监控画面</span>
          </div>
          <!-- 扫描线效果 -->
          <div class="scan-line"></div>
          <!-- 时间戳叠加 -->
          <div class="overlay-timestamp">{{ liveTime }}</div>
          <!-- LIVE 标识 -->
          <div class="overlay-live">
            <span class="live-dot"></span>
            LIVE
          </div>
          <!-- 摄像头信息 -->
          <div class="overlay-info">
            <div class="info-row">{{ camera?.cameraName }}</div>
            <div class="info-row">{{ camera?.location || '未知位置' }}</div>
          </div>
        </div>
      </div>
      <!-- 底部状态栏 -->
      <div class="monitor-footer">
        <span class="status-item">
          <el-tag :type="camera?.status === 'ONLINE' ? 'success' : 'info'" size="small">
            {{ camera?.status === 'ONLINE' ? '在线' : (camera?.status === 'FAULT' ? '故障' : '离线') }}
          </el-tag>
        </span>
        <span class="status-item">模拟监控 · 非真实画面</span>
      </div>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'CameraMonitor',
  props: {
    modelValue: { type: Boolean, default: false },
    camera: { type: Object, default: null },
  },
  emits: ['update:modelValue'],
  data() {
    return {
      liveTime: '',
      ticker: null,
      imageSeed: 0,
      imgError: false,
    }
  },
  computed: {
    visible: {
      get() {
        return this.modelValue
      },
      set(v) {
        this.$emit('update:modelValue', v)
      },
    },
    feedImageUrl() {
      const id = this.camera?.cameraId || 0
      const seed = (id * 1000 + this.imageSeed) % 10000
      return `https://picsum.photos/seed/${seed}/640/360`
    },
  },
  watch: {
    modelValue(val) {
      if (val) {
        this.imgError = false
        this.startLive()
      } else {
        this.stopLive()
      }
    },
  },
  beforeUnmount() {
    this.stopLive()
  },
  methods: {
    startLive() {
      this.updateTime()
      this.ticker = setInterval(() => {
        this.updateTime()
        this.imageSeed = Math.floor(Date.now() / 15000) % 10
      }, 1000)
    },
    stopLive() {
      if (this.ticker) {
        clearInterval(this.ticker)
        this.ticker = null
      }
    },
    updateTime() {
      const d = new Date()
      const pad = n => String(n).padStart(2, '0')
      this.liveTime = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    onClose() {
      this.stopLive()
    },
  },
}
</script>

<style scoped>
.camera-monitor-dialog :deep(.el-dialog__header) {
  background: #15151a;
  color: #fff;
  border-bottom: 1px solid #2a2a35;
}

.camera-monitor-dialog :deep(.el-dialog__title),
.camera-monitor-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}

.camera-monitor-dialog :deep(.el-dialog__body) {
  padding: 0;
  background: #0a0a0f;
}

.monitor-container {
  background: #0a0a0f;
  border-radius: 8px;
  overflow: hidden;
}

.monitor-frame {
  position: relative;
  aspect-ratio: 16 / 9;
  max-height: 70vh;
  background: #111;
}

.video-area {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.feed-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.feed-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  color: rgba(255, 255, 255, 0.4);
  font-size: 18px;
}

.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 0, 0, 0.03) 2px,
    rgba(0, 0, 0, 0.03) 4px
  );
  pointer-events: none;
}

.overlay-timestamp {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #0f0;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  padding: 4px 10px;
  border-radius: 4px;
}

.overlay-live {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(200, 0, 0, 0.85);
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  padding: 4px 10px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.live-dot {
  width: 6px;
  height: 6px;
  background: #fff;
  border-radius: 50%;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.overlay-info {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 4px;
}

.info-row {
  line-height: 1.5;
}

.monitor-footer {
  padding: 10px 16px;
  background: #15151a;
  color: #888;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-item {
  display: inline-flex;
  align-items: center;
}
</style>
