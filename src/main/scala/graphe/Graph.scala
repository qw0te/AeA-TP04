/**
 * Représente un graphe valué
 *
 * @constructor construit un nouveau graphe valué
 * @param vertices noeuds du graphe
 * @param edges arêtes du graphe
 *
 * @author Quentin Baert
 */
class Graph(val vertices: Set[Vertex], val edges: Set[Edge]) {

  ///////////////
  // ATTRIBUTS //
  ///////////////

  /**
   * Permet d'accéder aux noeuds du graphe par leur identifiant
   */
  val verticesId: Map[String, Vertex] =
    (this.vertices map (x => x.id -> x)).toMap

  //////////////
  // MÉTHODES //
  //////////////

  /**
   * Donne toutes les arêtes du graphe reliées à un sommet donné
   *
   * @param vertex noeud dont on souhaite récupérer les arêtes
   * @return arêtes du graphe reliées au sommet vertex
   */
  def getVertexEdges(vertex: Vertex): Set[Edge] =
    this.edges filter (e => (e.v1 == vertex) || (e.v2 == vertex))

  /**
   * Donne les succésseurs d'un sommet du graphe
   *
   * @param vertex sommet dont on souhaite récupérer les succésseurs
   * @return ensemble des succéseurs de vertex
   */
  def getVertexNeighbours(vertex: Vertex): Set[Vertex] =
    (this getVertexEdges vertex) map (e => e other vertex)

  /**
   * Détermine si le graphe est un graphe connexe ou non
   *
   * @return true si le graphe est connexe, false sinon
   */
  def isConnex: Boolean =
    !(this.vertices exists (v => (this getVertexEdges v).isEmpty))

  /**
   * Retourne un nouveau graphe avec un noeud supplémentaire
   *
   * @param vertex noeud à ajouter au graphe
   * @return nouveau graphe avec un noeud supplémentaire
   */
  def addVertex(vertex: Vertex): Graph =
    new Graph(this.vertices + vertex, this.edges)

  /**
   * Retourne un nouveau graphe avec une arête supplémentaire
   *
   * @param edge arête à ajouter au graphe
   * @return nouveau graphe avec une arête supplémentaire
   */
  def addEdge(edge: Edge): Graph =
    if ((this.vertices contains edge.v1) &&
        (this.vertices contains edge.v2))
      new Graph(this.vertices, this.edges + edge)
    else
      throw new Error("Graph.addEdge : Impossible d'ajouter l'arête")

  /**
   * Retourne un nouveau graphe avec une arête supplémentaire
   *
   * @param v1 première extrémitée de l'arête
   * @param v2 seconde extrémitée de l'arête
   * @param weight poids de l'arête
   * @return nouveau graphe avec une arête supplémentaire
   */
  def addEdgeBetween(v1: Vertex, v2: Vertex, weight: Int): Graph =
    this addEdge (new Edge(v1, v2, weight))

  override def toString: String =
    this.edges mkString "\n"

  /////////
  // MST //
  /////////

  /**
   * Donne l'arbre couvrant minimum du graphe grâce à l'algorithme de Prim
   *
   * @return arbre couvrant minimum du graphe
   */
  def getPrimMST: Graph = {
    // L'algorithme ainsi codé nécéssite que le graphe soit connexe
    require(this.isConnex)

    /*
     * v  : Ensemble des points marqués
     * e  : Ensemble des arêtes sortante de l'ensemble de points marqués
     * fe : Ensemble des arêtes à garder pour l'arbre couvrant minimum
     */
    def prim(v: Set[Vertex], e: Set[Edge], fe: Set[Edge]): Graph =
      if (this.vertices forall v.contains)
        new Graph(v, fe)
      else {
        // Arête avec le poids minimum
        val minEdge = e minBy (x => x.weight)
        // Extrémité de l'arête qui est déjà marqué
        val taggedVertex = if (v contains minEdge.v1) minEdge.v1 else minEdge.v2
        // Sommet à ajouter aux sommets marqués
        val vertex = minEdge other taggedVertex
        // Arêtes à ajouter à e
        val edgesToAdd =
          (this getVertexEdges vertex) filterNot (e => v contains (e other vertex))
        // Nouvel ensemble de sommets à considérer
        val newV = v + vertex
        // Nouvel ensemble d'arêtes à considérer
        val newE =
          (e ++ edgesToAdd) filterNot (
            e => (newV contains e.v1) && (newV contains e.v2)
          )

        prim(newV, newE, fe + minEdge)
      }

    val vertex = this.vertices.head
    val vertexEdges = this getVertexEdges vertex

    prim(Set(vertex), vertexEdges, Set())
  }

  /**
   * Donne l'arbre couvrant minimum du graphe grâce à l'algorithme de Kruskal
   *
   * @return arbre couvrant minimum du graphe
   */
  def getKruskalMST: Graph = {
    /*
     * sets        : Ensemble des sommets du graphe, chacun associé à un identifiant
     * edges       : arête à ajouter à l'arbre couvrant
     * unusedEdges : arête à potentiellement ajouter à l'arbre couvrant
     */
    def kruskal(sets: List[(Vertex, Int)], edges: Set[Edge], unusedEdges: List[Edge]): Graph =
      // Si toutes les arêtes ont été parcourues, le graphe peut être renvoyé
      if (unusedEdges.isEmpty)
        new Graph(this.vertices, edges)
      else {
        // Fonction qui trouve l'identifiant d'un sommet
        def findSetOf(v: Vertex): Int = (sets filter (c => c._1 == v)).head._2
        // Arête à considérer pour cette itération
        val edge = unusedEdges.head
        // Extrémités de l'arête à considérer
        val (v1, v2) = (edge.v1, edge.v2)
        // Identifiants des extrémités
        val (i1, i2) = (findSetOf(v1), findSetOf(v2))

        // Si les identifiants sont différents, on peut ajouter l'arête (pas de cycle)
        if (i1 != i2) {
          /*
           * Les identifiants sont mis à jours :
           * Tous les sommets qui avaient l'identifiant i2 ont maintenant
           * l'identifiant i1
           */
          val newSets = sets map (c => if (c._2 == i2) (c._1, i1) else c)
          kruskal(newSets, edges + edge, unusedEdges.tail)
        }
        // Sinon, ajouter l'arête créerait un cycle, elle n'est pas ajoutée
        else
          kruskal(sets, edges, unusedEdges.tail)
      }

    // Chaque sommet possède son propre identifiant
    val initSets = (this.vertices zip (1 to this.vertices.size)).toList
    val orderedEdges = this.edges.toList sortWith ((e1, e2) => e1.weight < e2.weight)

    kruskal(initSets, Set(), orderedEdges)
  }

}
