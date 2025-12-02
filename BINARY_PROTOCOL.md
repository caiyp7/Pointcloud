# 点云数据二进制协议格式

## 协议版本
- 当前：字符串格式（ASCII）
- 新版本：二进制格式（Binary）

## 二进制格式设计

### 数据布局（小端序 Little-Endian）

```
[Header: 48 bytes]
├─ uint32 (4 bytes): 点云点数 N
├─ uint32 (4 bytes): Frontier 字节数 F
├─ float32[3] (12 bytes): odom (x, y, z)
├─ float32[4] (16 bytes): quaternion (x, y, z, w)
└─ float32[3] (12 bytes): rc_goal (x, y, z)

[Point Cloud Data: 12*N bytes]
└─ float32[3*N]: 点云坐标 (x₁, y₁, z₁, x₂, y₂, z₂, ...)

[Frontier Data: F bytes]
└─ uint8[F]: UTF-8 字符串（如果 Frontier 是点云，也可以改为 float32[3*M]）
```

### 总大小
- Header: 48 字节（固定）
- Point Cloud: 12 * N 字节（N = 点云点数）
- Frontier: F 字节（F = Frontier 字符串长度）

### 示例
假设有 1000 个点，Frontier 字符串 50 字节：
- Header: 48 字节
- Point Cloud: 12,000 字节
- Frontier: 50 字节
- **总计**: 12,098 字节

相比字符串格式（约 50,000+ 字节），压缩率约 75%

## 兼容性
- 前端已支持字符串和二进制格式（自动检测）
- 后端可以添加一个开关，选择发送格式

## 后端实现指南（Kotlin）

### 需要修改的文件
- `UdpToWebSocketService.kt` 的 `buildPcStrFromDecoded` 函数（第152行）

### 实现步骤

#### 1. 创建二进制打包函数

```kotlin
fun buildBinaryFromDecoded(
    pointCloud: List<Float>,  // 点云数据 [x1, y1, z1, x2, y2, z2, ...]
    frontier: String,          // Frontier 字符串
    odom: FloatArray,          // [x, y, z]
    quaternion: FloatArray,    // [x, y, z, w]
    rcGoal: FloatArray         // [x, y, z]
): ByteArray {
    val pointCount = pointCloud.size / 3
    val frontierBytes = frontier.toByteArray(Charsets.UTF_8)
    val frontierSize = frontierBytes.size
    
    // 计算总大小
    val headerSize = 48  // 固定头部大小
    val pointCloudSize = pointCount * 3 * 4  // float32[3*N] = 12*N 字节
    val frontierSizeBytes = frontierSize
    val totalSize = headerSize + pointCloudSize + frontierSizeBytes
    
    // 创建 ByteArray
    val buffer = ByteArray(totalSize)
    var offset = 0
    
    // 使用 ByteBuffer（小端序）
    val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
    
    // Header: 48 bytes
    bb.putInt(pointCount)        // uint32: 点云点数 (4 bytes)
    bb.putInt(frontierSize)       // uint32: Frontier 字节数 (4 bytes)
    
    // odom[3] (12 bytes)
    bb.putFloat(odom[0])
    bb.putFloat(odom[1])
    bb.putFloat(odom[2])
    
    // quaternion[4] (16 bytes)
    bb.putFloat(quaternion[0])
    bb.putFloat(quaternion[1])
    bb.putFloat(quaternion[2])
    bb.putFloat(quaternion[3])
    
    // rc_goal[3] (12 bytes)
    bb.putFloat(rcGoal[0])
    bb.putFloat(rcGoal[1])
    bb.putFloat(rcGoal[2])
    
    // Point Cloud: float32[3*N]
    for (i in pointCloud.indices) {
        bb.putFloat(pointCloud[i])
    }
    
    // Frontier: UTF-8 字符串
    bb.put(frontierBytes)
    
    return buffer
}
```

#### 2. 修改 WebSocket 发送逻辑

在发送数据的地方，添加格式选择：

```kotlin
// 添加配置开关
private val useBinaryFormat = true  // 或从配置文件读取

// 发送数据
if (useBinaryFormat) {
    val binaryData = buildBinaryFromDecoded(
        pointCloud, frontier, odom, quaternion, rcGoal
    )
    session.sendBinary(binaryData)  // 发送二进制
} else {
    val textData = buildPcStrFromDecoded(...)  // 原有字符串格式
    session.sendText(textData)
}
```

#### 3. 数据格式验证

确保：
- `odom` 是 `FloatArray(3)`
- `quaternion` 是 `FloatArray(4)`
- `rcGoal` 是 `FloatArray(3)`
- `pointCloud` 长度是 3 的倍数
- 所有浮点数保留 4 位小数（或直接使用 Float，前端会自动处理）

### 性能优势

- **传输大小**：约减少 75%（字符串 → 二进制）
- **解析速度**：前端无需 `split` 和 `parseFloat`，直接读取 TypedArray
- **内存占用**：减少临时字符串对象创建

### 测试建议

1. 先保持字符串格式，测试前端兼容性
2. 添加开关，可以切换格式
3. 逐步切换到二进制格式
4. 监控性能提升（传输大小、解析时间）

