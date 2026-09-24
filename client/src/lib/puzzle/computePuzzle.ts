import seedrandom from "seedrandom"

/**
 * A point in image coordinates, in pixels.
 */
export interface Point {
  x: number
  y: number
}

/**
 * How an image is divided into a rows x cols grid of pieces.
 */
export interface PuzzleFrame {
  /** Image size, in pixels. */
  width: number
  height: number
  /** Grid dimensions. */
  rows: number
  cols: number
  /** Size of one piece, in pixels. */
  cellWidth: number
  cellHeight: number
}

/**
 * Everything needed to render a puzzle: the grid geometry plus one
 * SVG path (`d` attribute) per piece, at `paths[row][col]`.
 */
export interface PuzzleResult {
  frame: PuzzleFrame
  paths: string[][]
}

/** A candidate grid considered when dividing an image (see computePuzzleFrame). */
interface GridCandidate {
  rows: number
  cols: number
  cellWidth: number
  cellHeight: number
  score: number
}

/** Direction of an interior edge: "vertical" separates columns, "horizontal" separates rows. */
type EdgeOrientation = "vertical" | "horizontal"

/** Which side of an edge the jigsaw tab bulges toward. */
type TabSign = 1 | -1

/** A jigsaw edge between two adjacent pieces: start corner, 13 tab points, end corner. */
type Edge = Point[]

/** All interior edges of one orientation, indexed `[row][col]`. */
type EdgeGrid = Edge[][]

/**
 * Normalized jigsaw tab shape in edge-local coordinates: `x` runs
 * along the edge from 0 (start corner) to 1 (end corner), `y` is
 * perpendicular (positive to the left of the edge direction).
 * Placing it on the grid scales it to the cell size; `sign` flips
 * which side the tab is on.
 */
const JIGSAW_TAB_SHAPE: readonly Point[] = [
  { x: 0.35, y: 0.0 }, // P1  (anchor before the first curve)
  { x: 0.37, y: 0.0 }, // C1a
  { x: 0.39, y: -0.06 }, // C1b
  { x: 0.4, y: 0.05 }, // E1
  { x: 0.41, y: 0.15 }, // C2a
  { x: 0.3, y: 0.2 }, // C2b
  { x: 0.5, y: 0.2 }, // E2 (tab tip)
  { x: 0.7, y: 0.2 }, // C3a
  { x: 0.59, y: 0.15 }, // C3b
  { x: 0.6, y: 0.05 }, // E3
  { x: 0.61, y: -0.06 }, // C4a
  { x: 0.63, y: 0.0 }, // C4b
  { x: 0.65, y: 0.0 }, // E4  (anchor after the last curve)
]

/**
 * Chooses the rows x cols grid for an image: big enough for
 * `pieceCount` pieces, with cells as close to square as possible.
 *
 * Starts from the ideal (possibly fractional) grid whose cell aspect
 * ratio is exactly the image's, then scores nearby integer grids by
 * `excess pieces + distortion * distortionPenalty`, where distortion
 * is how far the cell aspect ratio is from 1.
 */
function computePuzzleFrame(
  imageWidth: number,
  imageHeight: number,
  pieceCount: number,
  distortionPenalty = 10,
): PuzzleFrame {
  if (imageWidth <= 0 || imageHeight <= 0) {
    throw new Error(`image dimensions must be positive, got ${imageWidth}x${imageHeight}`)
  }
  if (!Number.isInteger(pieceCount) || pieceCount < 1 || pieceCount > 1000) {
    throw new Error(`pieceCount must be a positive integer and smaller than 1000, got ${pieceCount}`)
  }

  const imageRatio = imageWidth / imageHeight
  const idealRows = Math.sqrt(pieceCount / imageRatio)
  const idealCols = Math.sqrt(pieceCount * imageRatio)

  if (Number.isInteger(idealRows) && Number.isInteger(idealCols)) {
    return makeFrame(imageWidth, imageHeight, idealRows, idealCols)
  }

  const rowRange = [
    Math.floor(idealRows) - 1,
    Math.floor(idealRows),
    Math.ceil(idealRows),
    Math.ceil(idealRows) + 1,
  ]
  const colRange = [
    Math.floor(idealCols) - 1,
    Math.floor(idealCols),
    Math.ceil(idealCols),
    Math.ceil(idealCols) + 1,
  ]

  let best: GridCandidate | null = null
  for (const rows of rowRange) {
    for (const cols of colRange) {
      if (rows * cols < pieceCount) continue

      const excess = rows * cols - pieceCount
      const cellWidth = imageWidth / cols
      const cellHeight = imageHeight / rows
      const distortion = Math.abs(cellWidth / cellHeight - 1)
      const score = excess + distortion * distortionPenalty

      if (best === null || score < best.score) {
        best = { rows, cols, cellWidth, cellHeight, score }
      }
    }
  }

  // Unreachable for valid pieceCount (the ceil+1 grid always fits); kept as a guard.
  if (best === null) {
    throw new Error(`no grid fits ${pieceCount} pieces`)
  }

  return makeFrame(imageWidth, imageHeight, best.rows, best.cols)
}

function makeFrame(width: number, height: number, rows: number, cols: number): PuzzleFrame {
  return {
    width,
    height,
    rows,
    cols,
    cellWidth: width / cols,
    cellHeight: height / rows,
  }
}

/**
 * Computes the pixel coordinates of one interior edge: the tab shape
 * placed between cell (i, j) and the neighbor it separates, scaled to
 * the cell size and flipped to `sign`'s side.
 */
function joinEdge(
  frame: PuzzleFrame,
  i: number,
  j: number,
  sign: TabSign,
  orientation: EdgeOrientation,
): Edge {
  // Every edge ends at the bottom-right corner of cell (i, j).
  const p2: Point = {
    x: frame.cellWidth * (j + 1),
    y: frame.cellHeight * (i + 1),
  }

  // Vertical edges run along the right side of cell (i, j), top to
  // bottom; horizontal edges run along its bottom side, left to right.
  const p0: Point =
    orientation === "vertical"
      ? { x: frame.cellWidth * (j + 1), y: frame.cellHeight * i }
      : { x: frame.cellWidth * j, y: frame.cellHeight * (i + 1) }

  const dir: Point = { x: p2.x - p0.x, y: p2.y - p0.y } // edge direction
  const perp: Point = { x: dir.y, y: -dir.x } // perpendicular, 90° clockwise

  const edge: Edge = [p0]
  for (const p of JIGSAW_TAB_SHAPE) {
    edge.push({
      x: p0.x + p.x * dir.x + p.y * sign * perp.x,
      y: p0.y + p.x * dir.y + p.y * sign * perp.y,
    })
  }
  edge.push(p2)

  return edge
}

/**
 * Generates every interior edge of the puzzle with random tab sides.
 * Deterministic for a given seed: all vertical edges first (row by
 * row), then all horizontal edges.
 */
function joinEdges(frame: PuzzleFrame, seed = 3108): { vertical: EdgeGrid; horizontal: EdgeGrid } {
  const rng = seedrandom(String(seed))
  const randomSign = (): TabSign => (rng() > 0.5 ? 1 : -1)

  const vertical: EdgeGrid = []
  for (let i = 0; i < frame.rows; i++) {
    vertical.push([])
    for (let j = 0; j < frame.cols - 1; j++) {
      vertical[i].push(joinEdge(frame, i, j, randomSign(), "vertical"))
    }
  }

  const horizontal: EdgeGrid = []
  for (let i = 0; i < frame.rows - 1; i++) {
    horizontal.push([])
    for (let j = 0; j < frame.cols; j++) {
      horizontal[i].push(joinEdge(frame, i, j, randomSign(), "horizontal"))
    }
  }

  return { vertical, horizontal }
}

/** A straight border side, in the same two-point edge format. */
function straight(from: Point, to: Point): Point[] {
  return [from, to]
}

/**
 * Formats one side of a piece as SVG path commands. Border sides (two
 * points) are straight lines; interior sides (15 points) follow the
 * tab shape's four cubic Bézier curves. `startCmd` is "M" for the
 * first side of a piece and "L" for the remaining sides.
 */
function edgePath(edge: Point[], startCmd: "M" | "L"): string {
  let path = `${startCmd} ${edge[0].x} ${edge[0].y} L ${edge[1].x} ${edge[1].y}`

  if (edge.length > 2) {
    // Only the first "C" needs a leading space: every later command
    // starts with its own letter ("C x y, ..." / "L x y").
    path += ` C ${edge[2].x} ${edge[2].y}, ${edge[3].x} ${edge[3].y}, ${edge[4].x} ${edge[4].y}`
    for (let k = 5; k + 2 < edge.length; k += 3) {
      path += `C ${edge[k].x} ${edge[k].y}, ${edge[k + 1].x} ${edge[k + 1].y}, ${edge[k + 2].x} ${edge[k + 2].y}`
    }
    path += `L ${edge[edge.length - 1].x} ${edge[edge.length - 1].y}`
  }

  return path
}

/**
 * Builds the SVG path (`d` attribute) of every piece. Each path traces
 * one piece clockwise — top, right, bottom, left — reusing interior
 * edges (reversed where the path runs against the edge's own
 * direction) and closing with "Z".
 */
function piecePaths(
  frame: PuzzleFrame,
  edges: { vertical: EdgeGrid; horizontal: EdgeGrid },
): string[][] {
  const { cellWidth, cellHeight } = frame

  const paths: string[][] = []
  for (let r = 0; r < frame.rows; r++) {
    paths.push([])
    for (let c = 0; c < frame.cols; c++) {
      const top: Point[] =
        r === 0
          ? straight({ x: c * cellWidth, y: 0 }, { x: c * cellWidth + cellWidth, y: 0 })
          : edges.horizontal[r - 1][c]
      const right: Point[] =
        c === frame.cols - 1
          ? straight(
              { x: frame.cols * cellWidth, y: r * cellHeight },
              { x: frame.cols * cellWidth, y: r * cellHeight + cellHeight },
            )
          : edges.vertical[r][c]
      // Bottom and left sides run right-to-left and bottom-to-top, so
      // their shared edges are traversed in reverse.
      const bottom: Point[] =
        r === frame.rows - 1
          ? straight(
              { x: c * cellWidth + cellWidth, y: frame.rows * cellHeight },
              { x: c * cellWidth, y: frame.rows * cellHeight },
            )
          : [...edges.horizontal[r][c]].reverse()
      const left: Point[] =
        c === 0
          ? straight({ x: 0, y: r * cellHeight + cellHeight }, { x: 0, y: r * cellHeight })
          : [...edges.vertical[r][c - 1]].reverse()

      paths[r].push(
        edgePath(top, "M") +
          " " +
          edgePath(right, "L") +
          " " +
          edgePath(bottom, "L") +
          " " +
          edgePath(left, "L") +
          "Z",
      )
    }
  }
  return paths
}

/**
 * Generates a jigsaw puzzle layout for an image.
 *
 * @param imageWidth  image width, in pixels
 * @param imageHeight image height, in pixels
 * @param pieceCount  approximate number of pieces (the best-fitting
 *                    grid may have a few more)
 * @param seed        seed for the tab directions; the same seed always
 *                    produces the same puzzle
 * @returns the grid geometry and one SVG path per piece, at
 *          `paths[row][col]`, ready to use as an SVG `d` attribute
 */
export function generatePuzzle(
  imageWidth: number,
  imageHeight: number,
  pieceCount: number,
  seed = 3108,
): PuzzleResult {
  const frame = computePuzzleFrame(imageWidth, imageHeight, pieceCount)
  const edges = joinEdges(frame, seed)
  return { frame, paths: piecePaths(frame, edges) }
}
