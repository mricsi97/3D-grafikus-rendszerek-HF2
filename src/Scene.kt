import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec2
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Vec4
import vision.gears.webglmath.Mat4
import kotlin.js.Date

class Scene (
  val gl : WebGL2RenderingContext) : UniformProvider("scene") {

  val vsTrafo = Shader(gl, GL.VERTEX_SHADER, "shaders/trafo-vs.glsl")
  val fsSolid = Shader(gl, GL.FRAGMENT_SHADER, "shaders/solid-fs.glsl")
  val fsTextured = Shader(gl, GL.FRAGMENT_SHADER, "shaders/textured-fs.glsl")  
  val solidProgram = Program(gl, vsTrafo, fsSolid)
  val texturedProgram = Program(gl, vsTrafo, fsTextured)  
  val quadGeometry = TexturedQuadGeometry(gl)

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame
  
  val asteroidMaterial = Material(texturedProgram)
  val landerMaterial = Material(texturedProgram)
  val yellowMaterial = Material(solidProgram)
  val cyanMaterial = Material(solidProgram)
  
  val yellowQuad = Mesh(yellowMaterial, quadGeometry)
  val cyanQuad = Mesh(cyanMaterial, quadGeometry)
  
  val gameObjects = ArrayList<GameObject>()
  
  val yellowQuadObject = GameObject(yellowQuad, Vec3(-0.6f, 0.3f, 0.0f), 0.0f, Vec3(0.2f, 0.2f, 0.0f))
  val cyanQuadObject = GameObject(cyanQuad, Vec3(-0.6f, 0.7f, 0.0f), 0.0f, Vec3(0.2f, 0.2f, 0.0f))
  
  val slowMovingAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry)){
	val velocity = Vec3(0.001f, 0.001f)
	override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
		position += velocity * dt
		return true
	}
  }
  
  val fastMovingAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry)){
	val velocity = Vec3(0.01f, 0.01f)
	override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
		position += velocity * dt
		return true
	}
  }
  
  val rollingAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry)){
	val rollSpeed = 0.01f
	override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
		roll += rollSpeed
		return true
	}
  }
  
  val rollableAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry)){
  	val rollSpeed = 0.1f
	override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
		if(keysPressed.contains("A"))
			roll += rollSpeed
		if(keysPressed.contains("D"))
			roll -= rollSpeed
		return true
	}
  }
  
  val camera = object: OrthoCamera(*Program.all) {}

  init{
  
  yellowMaterial["solidColor"]?.set(Vec4(1.0f, 1.0f, 0.0f, 1.0f))
  cyanMaterial["solidColor"]?.set(Vec4(0.0f, 1.0f, 1.0f, 1.0f))
  
  asteroidMaterial["colorTexture"]?.set(Texture2D(gl, "media/asteroid.png"))
  landerMaterial["colorTexture"]?.set(Texture2D(gl, "media/lander.png"))
  
  gameObjects.add(yellowQuadObject)
  gameObjects.add(cyanQuadObject)
  
  
  slowMovingAsteroid.position.set(0.0f, -0.5f)
  slowMovingAsteroid.scale.set(0.1f, 0.1f)
  fastMovingAsteroid.position.set(0.0f, -0.3f)
  fastMovingAsteroid.scale.set(0.1f, 0.1f)
  gameObjects.add(slowMovingAsteroid)
  gameObjects.add(fastMovingAsteroid)
  
  rollingAsteroid.scale.set(0.1f, 0.1f)
  gameObjects.add(rollingAsteroid)
  
  rollableAsteroid.scale.set(0.1f, 0.1f)
  rollableAsteroid.position.set(0.5f, -0.8f)
  gameObjects.add(rollableAsteroid)
  
  gl.enable(GL.BLEND)
  gl.blendFunc( GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA)
  addComponentsAndGatherUniforms(*Program.all)
  }

  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
	
	camera.setAspectRatio(2.370f)
  }

  @Suppress("UNUSED_PARAMETER")
  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>) {

    val dt = (Date().getTime().toFloat() - timeAtLastFrame.toFloat()) / 1000.0f
    val t  = (Date().getTime().toFloat() - timeAtFirstFrame.toFloat()) / 1000.0f    

    // clear the screen
    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)


	asteroidMaterial.draw()
	gl.useProgram(texturedProgram.glProgram)
    gl.uniformMatrix4fv(
      gl.getUniformLocation(
        texturedProgram.glProgram, "gameObject.modelMatrix"),
      false,
      Float32Array(arrayOf<Float>(
                        0.2f, 0.0f, 0.0f, -0.1f,
                        0.0f, 0.2f, 0.0f, 0.3f,
                        0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 0.0f, 0.0f, 1.0f
      )))
	quadGeometry.draw()
	
	
	landerMaterial.draw()
	gl.useProgram(texturedProgram.glProgram)
	gl.uniformMatrix4fv(
      gl.getUniformLocation(
        texturedProgram.glProgram, "gameObject.modelMatrix"),
      false,
      Float32Array(arrayOf<Float>(
                        0.2f, 0.0f, 0.0f, -0.1f,
                        0.0f, 0.2f, 0.0f, 0.7f,
                        0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 0.0f, 0.0f, 1.0f
      )))
	quadGeometry.draw()
	
	
	gl.useProgram(solidProgram.glProgram)
	gl.uniformMatrix4fv(
      gl.getUniformLocation(
        solidProgram.glProgram, "gameObject.modelMatrix"),
      false,
      Float32Array(arrayOf<Float>(
                        0.2f, 0.0f, 0.0f, 0.5f,
                        0.0f, 0.2f, 0.0f, 0.7f,
                        0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 0.0f, 0.0f, 1.0f
    )))
	yellowQuad.draw()
	
	
	gl.useProgram(solidProgram.glProgram)
	gl.uniformMatrix4fv(
      gl.getUniformLocation(
        solidProgram.glProgram, "gameObject.modelMatrix"),
      false,
      Float32Array(arrayOf<Float>(
                        0.2f, 0.0f, 0.0f, 0.5f,
                        0.0f, 0.2f, 0.0f, 0.3f,
                        0.0f, 0.0f, 1.0f, 0.0f,
                        0.0f, 0.0f, 0.0f, 1.0f
    )))
	cyanQuad.draw()

	
	camera.position = slowMovingAsteroid.position.xy
	camera.updateViewProjMatrix()
	
	
	gameObjects.forEach { it.move(t, dt, keysPressed, gameObjects) }
	gameObjects.forEach { it.update() }
	gameObjects.forEach { it.draw(camera) }


  }
}
