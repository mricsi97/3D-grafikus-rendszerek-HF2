import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Mat4
import vision.gears.webglmath.Vec3Array

open class GameObject(
  mesh : Mesh,
  val radius : Float = 0.0f,
  val position : Vec3 = Vec3.zeros.clone(),
  var roll : Float = 0.0f,
  val scale : Vec3 = Vec3.ones.clone()
   ) : UniformProvider("gameObject") {
   
   val modelMatrix by Mat4()
   
 init { 
    addComponentsAndGatherUniforms(mesh)
  }
  
   fun update() {
   
   modelMatrix.set().
      scale(scale).rotate(roll).translate(position)
	  
 }
 
 open fun move(
	dt : Float = 0.016666f,
	t : Float = 0.0f,
	keysPressed : Set<String> = emptySet<String>(),
	gameObjects : List<GameObject> = emptyList<GameObject>()
	) : Boolean {
	return true
	}

  open fun collide(){}

}