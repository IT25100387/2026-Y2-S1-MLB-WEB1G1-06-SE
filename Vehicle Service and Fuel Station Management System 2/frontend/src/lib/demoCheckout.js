import {createElement} from 'react';
import {createRoot} from 'react-dom/client';
import DemoCardCheckout from '../components/DemoCardCheckout';

export function openDemoCardCheckout(checkout,path){
  return new Promise((resolve,reject)=>{
    const host=document.createElement('div');document.body.appendChild(host);const root=createRoot(host);
    const finish=callback=>value=>{setTimeout(()=>{root.unmount();host.remove();callback(value);},0);};
    root.render(createElement(DemoCardCheckout,{checkout,path,onFinish:finish(resolve),onCancel:finish(()=>reject(new Error('Demo checkout cancelled. No money was collected.')))}));
  });
}
