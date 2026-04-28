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

// Sensitivity: minimum pixel distance from centre for a click to affect rotation.
const SENSITIVITY = 50;
// Reference canvas size (same as Java version); used to compute scale factor.
const REF_SIZE = 350;

const canvas      = document.getElementById('canvas') as HTMLCanvasElement;
const bodySelect  = document.getElementById('body-select') as HTMLSelectElement;
const colourSelect = document.getElementById('colour-select') as HTMLSelectElement;
const effectSelect = document.getElementById('effect-select') as HTMLSelectElement;
const ctx         = canvas.getContext('2d', { willReadFrequently: true })!;

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

// --- Shape loading ---

function shapeUrl(name: string): string {
  return import.meta.env.BASE_URL + 'shapes/' + name + '.body';
}

async function loadShape(name: string, colour?: Colour): Promise<void> {
  const col = colour ?? body?.getColour() ?? COLOURS.blue;
  const loaded = await Body.load(shapeUrl(name), col);

  if (name === 'mu') {
    for (let i = 0; i < 60; i++) loaded.rotateZY();
  } else if (name === 'torus') {
    for (let i = 0; i < 5; i++) loaded.rotateXZ();
    for (let i = 0; i < 5; i++) loaded.rotateYZ();
  }

  body = loaded;
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

// --- Menu events ---

bodySelect.addEventListener('change', () => {
  loadShape(bodySelect.value).catch(console.error);
});

colourSelect.addEventListener('change', () => {
  if (body) {
    body.setColour(COLOURS[colourSelect.value] ?? COLOURS.blue);
    repaint();
  }
});

// --- Startup ---

async function init(): Promise<void> {
  resizeCanvas();

  effect   = new ElasticEffect(repaint);
  renderer.setEffect(effect);

  await loadShape('mu', COLOURS.blue);

  animCtrl = new AnimationController(body, repaint);
  animCtrl.kickstart(0.5, 0.5);
}

init().catch(console.error);
