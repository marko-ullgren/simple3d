import { Body, COLOURS, type Colour } from './model/Body.js';
import { StarField } from './render/StarField.js';
import { Renderer } from './render/Renderer.js';
import { ElasticEffect } from './render/effect/ElasticEffect.js';
import type { Effect } from './render/effect/Effect.js';
import { NoEffect } from './render/effect/NoEffect.js';
import { RippleEffect } from './render/effect/RippleEffect.js';
import { VortexEffect } from './render/effect/VortexEffect.js';
import { ShockwaveEffect } from './render/effect/ShockwaveEffect.js';
import { AnimationController } from './control/AnimationController.js';
import { NoTexture } from './render/texture/NoTexture.js';
import { StoneTexture } from './render/texture/StoneTexture.js';
import { MetalTexture } from './render/texture/MetalTexture.js';

// Sensitivity: minimum pixel distance from centre for a click to affect rotation.
const SENSITIVITY = 50;
// Reference canvas size (same as Java version); used to compute scale factor.
const REF_SIZE = 350;

const canvas      = document.getElementById('canvas') as HTMLCanvasElement;
const bodySelect  = document.getElementById('body-select') as HTMLSelectElement;
const colourInput  = document.getElementById('colour-input') as HTMLInputElement;
const effectSelect = document.getElementById('effect-select') as HTMLSelectElement;
const textureSelect = document.getElementById('texture-select') as HTMLSelectElement;
const ctx         = canvas.getContext('2d')!;

const starField = new StarField();
const renderer  = new Renderer();

let body:   Body;
let animCtrl: AnimationController;
let effect: Effect;
let zoom = 1.0;
let rafPending = false;

function repaint(): void {
  if (!rafPending) {
    rafPending = true;
    requestAnimationFrame(() => {
      rafPending = false;
      render();
    });
  }
}

function render(): void {
  const w = canvas.width;
  const h = canvas.height;
  const cx = w / 2;
  const cy = h / 2;
  const scale = zoom * Math.min(w, h) / REF_SIZE;

  ctx.fillStyle = '#000';
  ctx.fillRect(0, 0, w, h);
  starField.draw(ctx, w, h);

  if (body) {
    renderer.render(body, ctx, Math.round(cx), Math.round(cy), scale, w, h);
  }
}

// --- Colour helpers ---

function hexToColour(hex: string): Colour {
  return {
    r: parseInt(hex.slice(1, 3), 16),
    g: parseInt(hex.slice(3, 5), 16),
    b: parseInt(hex.slice(5, 7), 16),
  };
}

function colourToHex(c: Colour): string {
  return '#' + [c.r, c.g, c.b]
    .map(v => v.toString(16).padStart(2, '0'))
    .join('');
}

// --- Shape loading ---

function shapeUrl(name: string): string {
  return import.meta.env.BASE_URL + 'shapes/' + name + '.body';
}

async function loadBodyList(): Promise<string[]> {
  const url = import.meta.env.BASE_URL + 'shapes/bodies.list';
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to load bodies.list (${res.status})`);
  return (await res.text())
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0 && !l.startsWith('#'));
}

function populateBodySelect(names: string[]): void {
  bodySelect.innerHTML = '';
  for (const name of names) {
    const option = document.createElement('option');
    option.value = name;
    option.textContent = name.charAt(0).toUpperCase() + name.slice(1);
    bodySelect.appendChild(option);
  }
}

async function loadShape(name: string, colour?: Colour): Promise<void> {
  const col = colour ?? body?.getColour() ?? COLOURS.blue;
  body = await Body.load(shapeUrl(name), col);
  colourInput.value = colourToHex(col);
  if (animCtrl) {
    animCtrl.setBody(body);
  }
  repaint();
}

// --- Canvas resize ---

function resizeCanvas(): void {
  canvas.width  = canvas.clientWidth;
  canvas.height = canvas.clientHeight;
  repaint();
}

window.addEventListener('resize', resizeCanvas);

// --- Mouse events ---

canvas.addEventListener('mousedown', (e) => {
  const rect = canvas.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  animCtrl?.applyImpulse(x, y, canvas.width / 2, canvas.height / 2, SENSITIVITY);
  effect?.trigger(x, y);
});

canvas.addEventListener('wheel', (e) => {
  e.preventDefault();
  zoom *= Math.pow(1.1, -e.deltaY / 100);
  zoom = Math.max(0.1, Math.min(zoom, 10));
  repaint();
}, { passive: false });

// --- Effect switching ---

function switchEffect(next: Effect): void {
  effect.stop();
  effect = next;
  renderer.setEffect(effect);
}

effectSelect.addEventListener('change', () => {
  switch (effectSelect.value) {
    case 'elastic':   switchEffect(new ElasticEffect(repaint));   break;
    case 'ripple':    switchEffect(new RippleEffect(repaint));    break;
    case 'vortex':    switchEffect(new VortexEffect(repaint));    break;
    case 'shockwave': switchEffect(new ShockwaveEffect(repaint)); break;
    case 'none':      switchEffect(new NoEffect());               break;
  }
});

// --- Texture switching ---

textureSelect.addEventListener('change', () => {
  switch (textureSelect.value) {
    case 'stone':
      renderer.setWireframeMode(false);
      renderer.setTexture(new StoneTexture());
      break;
    case 'metal':
      renderer.setWireframeMode(false);
      renderer.setTexture(new MetalTexture());
      break;
    case 'wireframe':
      renderer.setWireframeMode(true);
      break;
    case 'none':
    default:
      renderer.setWireframeMode(false);
      renderer.setTexture(new NoTexture());
      break;
  }
  repaint();
});

// --- Menu events ---

bodySelect.addEventListener('change', () => {
  loadShape(bodySelect.value).catch(console.error);
});

colourInput.addEventListener('input', () => {
  if (body) {
    body.setColour(hexToColour(colourInput.value));
    repaint();
  }
});

// --- Startup ---

async function init(): Promise<void> {
  resizeCanvas();

  effect   = new ElasticEffect(repaint);
  renderer.setEffect(effect);

  const names = await loadBodyList();
  populateBodySelect(names);

  await loadShape(names[0], COLOURS.blue);

  animCtrl = new AnimationController(body, repaint);
  animCtrl.kickstart(0.5, 0.5);
}

init().catch(console.error);
