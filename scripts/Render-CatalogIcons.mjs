// Adapt downloaded SVG icon paths to small local catalog illustrations.
import {chromium} from '../frontend/node_modules/playwright-core/index.mjs';
import {readFile,writeFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
const root=new URL('../images/catalog/',import.meta.url);
const sources=JSON.parse(await readFile(new URL('SOURCES.json',root),'utf8'));
const browser=await chromium.launch({channel:'msedge',headless:true});
try{
  const page=await browser.newPage();
  for(const {icon} of sources){
    const svg=await readFile(new URL(icon+'.svg',root),'utf8');
    const result=await page.evaluate(async({svg})=>{
      const parsed=new DOMParser().parseFromString(svg,'image/svg+xml').documentElement;
      const paths=[...parsed.querySelectorAll('path')].map(path=>path.getAttribute('d'));
      const [,,w,h]=parsed.getAttribute('viewBox').split(' ').map(Number);
      const scale=Math.min(168/w,168/h);
      const content=`<svg xmlns="http://www.w3.org/2000/svg" width="320" height="240"><rect width="320" height="240" fill="#24251f"/><circle cx="160" cy="120" r="91" fill="#d6aa6c" opacity=".05"/><g fill="#d6aa6c" transform="translate(${(320-w*scale)/2} ${(240-h*scale)/2}) scale(${scale})">${paths.map(d=>`<path d="${d}"/>`).join('')}</g></svg>`;
      const image=new Image();image.src='data:image/svg+xml;charset=utf-8,'+encodeURIComponent(content);await image.decode();
      const canvas=document.createElement('canvas');canvas.width=320;canvas.height=240;canvas.getContext('2d').drawImage(image,0,0);
      return canvas.toDataURL('image/png').split(',')[1];
    },{svg});
    await writeFile(new URL(icon+'.png',root),Buffer.from(result,'base64'));
  }
  console.log(`Rendered ${sources.length} catalog icon PNGs in ${fileURLToPath(root)}`);
}finally{await browser.close();}
