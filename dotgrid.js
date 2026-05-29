/**
 * @schema 2.11
 * @input gap: number = 22
 * @input dot: number = 1.6
 * @input color: color = #d4d8e2
 */
const gap = Math.max(8, pencil.input.gap);
const r = pencil.input.dot;
const cols = Math.ceil(pencil.width / gap) + 1;
const rows = Math.ceil(pencil.height / gap) + 1;
const nodes = [];
for (let y = 0; y < rows; y++) {
  for (let x = 0; x < cols; x++) {
    nodes.push({
      type: "ellipse",
      name: "d",
      x: x * gap,
      y: y * gap,
      width: r * 2,
      height: r * 2,
      fill: pencil.input.color,
    });
  }
}
return nodes;
