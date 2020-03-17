import org.khronos.webgl.WebGLRenderingContext as GL

abstract class VertexArray {}

abstract external class WebGL2RenderingContext : GL {

  fun drawBuffers(buffers: IntArray)
  fun drawElementsInstanced(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int)
  fun readBuffer(src: Int)
  fun renderbufferStorageMultisample(target: Int, samples: Int, internalformat: Int, width: Int, height: Int)
  fun vertexAttribDivisor(index: Int, divisor: Int)
  fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)
  fun createVertexArray() : VertexArray
  fun bindVertexArray(vertexArray: VertexArray?)
}

