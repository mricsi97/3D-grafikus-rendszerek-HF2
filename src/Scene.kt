import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL
import org.khronos.webgl.Float32Array
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec2
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Vec4
import vision.gears.webglmath.Mat4
import kotlin.js.Date
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.abs


class Scene (
  val gl : WebGL2RenderingContext) : UniformProvider("scene") {
  
  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  val vsTrafo = Shader(gl, GL.VERTEX_SHADER, "shaders/trafo-vs.glsl")
  val fsSolid = Shader(gl, GL.FRAGMENT_SHADER, "shaders/solid-fs.glsl")
  val fsTextured = Shader(gl, GL.FRAGMENT_SHADER, "shaders/textured-fs.glsl")  
  val solidProgram = Program(gl, vsTrafo, fsSolid)
  val texturedProgram = Program(gl, vsTrafo, fsTextured)  
  
  val quadGeometry = TexturedQuadGeometry(gl)
  val asteroidMaterial = Material(texturedProgram)
  val landerMaterial = Material(texturedProgram)
  val projectileMaterial = Material(texturedProgram)
  
  val gameObjects = ArrayList<GameObject>()
   
  val slowMovingAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry), 0.1f){
    val velocity = Vec3(0.1f, 0.1f)
    override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
      position += velocity * dt
      return true
    }
  }
  
  val fastMovingAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry), 0.1f){
    val velocity = Vec3(1.0f, 1.0f)
    override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
      position += velocity * dt
      return true
    }
  }
  
  val rollingAsteroid = object: GameObject(Mesh(asteroidMaterial, quadGeometry), 0.1f){
  	val rollSpeed = 0.01f
    override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
      roll += rollSpeed
      return true
    }
  }
  
  val lander = object: GameObject(Mesh(landerMaterial, quadGeometry), 0.1f) {
    val mass = 1.0f
    val invMass = 1.0f / mass
    val invAngularMass = Mat4(mass*0.0025f,          0.0f,           0.0f, 0.0f,
                                  0.0f,      mass*0.0025f,           0.0f, 0.0f,   // inverz tehetetlenségi nyomaték, I^(-1)
                                  0.0f,              0.0f,   mass*0.0025f, 0.0f,
                                  0.0f,              0.0f,           0.0f, 1.0f).invert()  
    val dragCoefficient = 1.5f                             // légellenállás, c
    var momentum = Vec3(0.0f, 0.0f, 0.0f)                  // lendület, p
    var angularMomentum = Vec3(0.0f, 0.0f, 0.0f)           // perdület, L
    var orientation =  Vec4()                              // forgatás (kvaternió), q
    val initialDirection = Vec4(0.0f, 1.0f, 0.0f, 0.0f)    // x,y,z,w; w=0

    val projectileCooldown = 5.0f
    var projectileCooldownCounter = 0.0f

    override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {
      projectileCooldownCounter -= dt

      val conjOrientation = Vec4(0.0f, 0.0f, -this.orientation.z, this.orientation.w)      // konjugált kvaternió, q*
      var direction = hamiltonProduct(hamiltonProduct(this.orientation, this.initialDirection), conjOrientation).normalize()

      var moveForce = Vec3(0.0f, 0.0f, 0.0f)
      var turnForce = Vec3(0.0f, 0.0f, 0.0f)

      if(keysPressed.contains("W"))
        moveForce.plusAssign(direction.xyz * 2.0f)
      if(keysPressed.contains("S"))
        moveForce.plusAssign(-direction.xyz * 2.0f)
      if(keysPressed.contains("A"))
        turnForce.plusAssign(Vec3(-direction.y * 2.0f, direction.x * 2.0f, 0.0f))
      if(keysPressed.contains("D"))
        turnForce.plusAssign(Vec3(direction.y * 2.0f, -direction.x * 2.0f, 0.0f))
      if(keysPressed.contains("SPACE"))
        if(projectileCooldownCounter <= 0){
          createProjectile(direction.xyz)
          projectileCooldownCounter = projectileCooldown
        }

      this.momentum.plusAssign(moveForce * dt)
      this.momentum.timesAssign(exp(-dt * dragCoefficient * invMass))
      val velocity = this.momentum * invMass              // sebesség, v
      this.position.plusAssign(velocity * dt)

      val torque = direction.xyz.cross(turnForce)         // forgatónyomaték, τ
      this.angularMomentum.plusAssign(torque * dt)
      this.angularMomentum.timesAssign(exp(-dt * dragCoefficient * invMass))
      val transModelMatrix = this.modelMatrix.clone().transpose()
      val angularVelocity = Vec4(this.angularMomentum, 0.0f) * transModelMatrix * invAngularMass * this.modelMatrix  // szögsebesség, ω
      val rotation = angularVelocity.xyz * dt
      this.orientation = hamiltonProduct(this.orientation, Vec4(0.0f, 0.0f, sin(rotation.z / 2.0f), cos(rotation.z / 2.0f)))
      direction = hamiltonProduct(hamiltonProduct(this.orientation, this.initialDirection), conjOrientation)

      this.roll = atan2(direction.y, direction.x) - PI.toFloat() / 2.0f
      return true
    }

    override fun collide() {
      for(i in gameObjects)
        if(this != i && circleCollision(this, i))
          this.destroy()
    }

    fun destroy(){
      gameObjects.remove(this)
    }
  }

  fun hamiltonProduct(q : Vec4, p : Vec4) : Vec4 {
	return Vec4(	q.w*p.x + q.x*p.w + q.y*p.z - q.z*p.y,
		          	q.w*p.y - q.x*p.z + q.y*p.w + q.z*p.x,
			          q.w*p.z + q.x*p.y - q.y*p.x + q.z*p.w,
		          	q.w*p.w - q.x*p.x - q.y*p.y - q.z*p.z)
  } 

  val camera = object: OrthoCamera(*Program.all) {}

  fun createProjectile(landerDirection : Vec3){
    val projectile = object : GameObject(Mesh(projectileMaterial, quadGeometry), 0.001f) {
      val enemyPosition = findClosestObject(lander.position)
      val mass = 0.2f
      val invMass = 1.0f / mass
      val dragCoefficient = 0.5f                             // légellenállás, c
      var velocity = Vec3(0.0f, 0.0f, 0.0f)                  // sebesség, v

      override fun move(dt : Float, t : Float, keysPressed : Set<String>, gameObjects : List<GameObject>) : Boolean {

        val direction = (enemyPosition - this.position).normalize()

        val moveForce = direction * 1.0f
        val acceleration = moveForce * invMass
        this.velocity.plusAssign(acceleration * dt)
        this.velocity.timesAssign(exp(-dt * dragCoefficient * invMass))
        this.position.plusAssign(this.velocity * dt)

        return true
      }

      override fun collide(){
        for(i in gameObjects)
          if(this != i && circleCollision(this, i))
              this.destroy()
      }
      fun destroy(){
        gameObjects.remove(this)
      }
    }

    projectile.position.set(lander.position - landerDirection * 0.155f)
    projectile.scale.set(0.01f, 0.01f, 0.01f)
    gameObjects.add(projectile)
  }

  fun findClosestObject(ownPosition : Vec3) : Vec3 {
    var closest = gameObjects[0].position
    for(i in gameObjects){
      if(ownPosition != i.position && (closest - ownPosition).length() > (i.position - ownPosition).length())
        closest = i.position
    }
    return closest
  }

  fun circleCollision(obj1 : GameObject, obj2 : GameObject) : Boolean {
    return (obj1.position - obj2.position).length() <= obj1.radius + obj2.radius
  }

  init{
  
  asteroidMaterial["colorTexture"]?.set(Texture2D(gl, "media/asteroid.png"))
  landerMaterial["colorTexture"]?.set(Texture2D(gl, "media/lander.png"))
  projectileMaterial["colorTexture"]?.set(Texture2D(gl, "media/projectile.png"))
  
  fastMovingAsteroid.position.set(-1.0f, -1.0f)
  fastMovingAsteroid.scale.set(0.1f, 0.1f)
  gameObjects.add(fastMovingAsteroid)

  slowMovingAsteroid.position.set(0.0f, -0.5f)
  slowMovingAsteroid.scale.set(0.1f, 0.1f)
  gameObjects.add(slowMovingAsteroid)
  
  rollingAsteroid.position.set(0.5f, -0.8f)
  rollingAsteroid.scale.set(0.1f, 0.1f)
  gameObjects.add(rollingAsteroid)

  lander.scale.set(0.1f, 0.1f, 0.1f)
  lander.position.set(-1.0f, 1.0f, 0.0f)
  gameObjects.add(lander)
  
  gl.enable(GL.BLEND)
  gl.blendFunc( GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA)
  addComponentsAndGatherUniforms(*Program.all)
  }

  fun resize(gl : WebGL2RenderingContext, canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)
	
	  camera.setAspectRatio(1.77777777778f)
  }

  @Suppress("UNUSED_PARAMETER")
  fun update(gl : WebGL2RenderingContext, keysPressed : Set<String>) {

    val timeAtThisFrame = Date().getTime()
    val dt = (timeAtThisFrame.toFloat() - timeAtLastFrame.toFloat()) / 1000.0f
    val t  = (timeAtThisFrame.toFloat() - timeAtFirstFrame.toFloat()) / 1000.0f
    timeAtLastFrame = timeAtThisFrame

    // clear the screen
    gl.clearColor(0.0f, 0.0f, 0.0f, 1.0f)
    gl.clearDepth(1.0f)
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
    
    camera.position = lander.position.xy
    camera.updateViewProjMatrix()
    

    gameObjects.forEach { it.move(dt, t, keysPressed, gameObjects) }
    gameObjects.forEach { it.collide() }
    gameObjects.forEach { it.update() }
    gameObjects.forEach { it.draw(camera) }
  }
}
