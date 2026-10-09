export function inventoryState(kind,value){
  const state=String(value||'').trim();
  if(kind==='fuel'&&/^(normal|optimal)$/i.test(state))return 'Optimal';
  if(kind==='parts'&&/^(available|in stock)$/i.test(state))return 'In Stock';
  return state||'Unavailable';
}

// Retain the existing comma-separated supplier field, including old/custom values.
export function parseGoods(value){
  value=String(value||'');
  const items=[];let item='',quoted=false;
  for(let i=0;i<value.length;i++){
    const char=value[i];
    if(char==='"'){if(quoted&&value[i+1]==='"'){item+='"';i++;}else quoted=!quoted;}
    else if(char===','&&!quoted){items.push(item.trim());item='';}
    else item+=char;
  }
  items.push(item.trim());return [...new Set(items.filter(Boolean))];
}
export function serializeGoods(items){return [...new Set(items.map(item=>item.trim()).filter(Boolean))].map(item=>/[,"\n]/.test(item)?'"'+item.replaceAll('"','""')+'"':item).join(', ');}
export const litres=value=>new Intl.NumberFormat('en-LK',{maximumFractionDigits:2}).format(Number(value)||0);
