import {test,expect} from '@playwright/test';

const invoice={id:1,invoiceNumber:'SP-100',invoiceType:'SPARE_PART',invoiceDate:'2026-10-08',customerName:'Guest Customer',status:'PENDING',balanceDue:100,amountPaid:0,netTotal:100,netTotalWithPenalty:100,finalized:true,lineItems:[]};
const products=Array.from({length:8},(_,i)=>({id:i+1,partName:'Engine oil '+(i+1),category:'Engine',stockQuantity:10,price:100,sellingPrice:100,available:true}));
const paidInvoice=()=>({...invoice,status:'PAID',balanceDue:0,amountPaid:100,refundedAmount:0});
const paidPayment={id:10,invoiceId:1,invoiceNumber:'SP-100',amount:100,refundedAmount:0,status:'SUCCESS',paymentMethod:'CASH'};
async function setup(page,role='ROLE_MANAGER',document=invoice){
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{window.payhere={startPayment(payment){window.gatewayPayments=(window.gatewayPayments||[]).concat(payment);window.payhere.onCompleted?.(payment.order_id);}};});
  await page.route('**/api/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    let data={};
    if(path==='/api/auth/me')data={role,user:{id:1,username:'tester',fullName:'Test User'}};
    else if(path==='/api/v1/notifications')data={notifications:[],unreadCount:0};
    else if(path==='/api/billing/invoices')data={invoices:[document]};
    else if(path==='/api/pos/invoices/today')data={invoices:[document],stationDate:'2026-10-08'};
    else if(path==='/api/billing/invoices/1')data={invoice:document,payments:[]};
    else if(path==='/api/pos/products')data=products;
    else if(path.endsWith('/configuration'))data={configured:true,provider:'PayHere',sandbox:true};
    else if(path==='/api/pos/quote'){
      const body=route.request().postDataJSON(),quantity=(body.items||[]).reduce((sum,r)=>sum+r.quantity,0);
      data={lines:(body.items||[]).map(r=>({partId:r.partId,itemName:'Engine oil '+r.partId,quantity:r.quantity,unitPrice:100,totalAmount:100*r.quantity})),subtotal:100*quantity,total:100*quantity,balance:100*quantity,paid:0,offerSavings:0,recordedDiscount:0,charges:0,finalized:true,quoteToken:'quote-100'};
    }
    await route.fulfill({json:data});
  });
  return errors;
}

test('cashier part search shows three compact yellow suggestion rows and supports keyboard selection',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');await page.goto('/dashboard/pos');
  const search=page.getByRole('combobox',{name:'Search spare parts'});await search.fill('Engine');
  const menu=page.getByRole('listbox',{name:'Matching spare parts'});await expect(menu).toBeVisible();await expect(menu.getByRole('option')).toHaveCount(8);
  const box=await menu.boundingBox();expect(box.height).toBe(98);
  await expect(menu.getByRole('option').first()).toHaveCSS('background-color','rgb(230, 188, 80)');
  await search.press('ArrowDown');await search.press('Enter');await expect(search).toHaveValue('Engine oil 2');await expect(menu).toBeHidden();await expect(page.getByRole('button',{name:'Add Engine oil 2',exact:true})).toBeVisible();expect(errors).toEqual([]);
});

test('invoice Pay offers only Cash Card QR and card goes directly to gateway, then waits for server confirmation',async({page})=>{
  const errors=await setup(page);let body,confirmed=false,manual=0;
  await page.route('**/api/billing/pay',async route=>{manual++;await route.fulfill({json:{success:true}});});
  await page.route('**/api/payments/card/start',async route=>{body=route.request().postDataJSON();await route.fulfill({json:{saleId:'checkout-100',state:'PENDING',payment:{order_id:'checkout-100',amount:'100.00'}}});});
  await page.route('**/api/payments/card/*',async route=>{if(new URL(route.request().url()).pathname.endsWith('configuration'))return route.fulfill({json:{configured:true}});await route.fulfill({json:{completed:confirmed,state:confirmed?'SUCCESS':'PENDING',invoice}});});
  // Register the specific start handler after the wildcard.
  await page.route('**/api/payments/card/start',async route=>{body=route.request().postDataJSON();await route.fulfill({json:{saleId:route.request().headers()['idempotency-key'],state:'PENDING',payment:{order_id:'checkout-100',amount:'100.00'}}});});
  await page.goto('/dashboard/invoices');await page.getByRole('button',{name:'Pay',exact:true}).click();
  await page.getByRole('combobox',{name:'Payment method',exact:true}).click();const menu=page.getByRole('listbox');
  expect((await menu.getByRole('option').allTextContents()).filter(t=>['CASH','CARD','QR'].includes(t))).toEqual(['CASH','CARD','QR']);await expect(page.getByRole('option',{name:/BANK|CHEQUE|OTHER/})).toHaveCount(0);
  await page.getByRole('option',{name:'CARD',exact:true}).click();await page.getByRole('button',{name:'Confirm payment',exact:true}).click();
  await expect.poll(()=>page.evaluate(()=>window.gatewayPayments?.length||0)).toBe(1);expect(body).toEqual({invoiceNumber:'SP-100',amount:100});expect(manual).toBe(0);
  await expect(page.getByText('Card billing details',{exact:true})).toHaveCount(0);await expect(page.getByRole('button',{name:'Waiting for PayHere confirmation…'})).toBeDisabled();
  confirmed=true;await expect(page.getByText('Process payment',{exact:true})).toBeHidden({timeout:7000});await expect(page.getByText('Receipt ready',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Print receipt',exact:true})).toBeVisible();expect(errors).toEqual([]);
});

test('POS card checkout sends only the checkout and opens no customer billing form',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');let body;
  await page.route('**/api/pos/card/start',async route=>{body=route.request().postDataJSON();await route.fulfill({json:{saleId:'checkout-100',state:'PENDING',payment:{order_id:'checkout-100',amount:'100.00'}}});});
  await page.route('**/api/pos/card/checkout-100',route=>route.fulfill({json:{completed:false,state:'PENDING'}}));
  await page.goto('/dashboard/pos');await page.getByRole('button',{name:'Add Engine oil 1',exact:true}).click();await page.getByRole('button',{name:'Card',exact:true}).click();
  const pay=page.getByRole('button',{name:'Continue to card payment',exact:true});await expect(pay).toBeEnabled();await pay.click();
  await expect.poll(()=>page.evaluate(()=>window.gatewayPayments?.length||0)).toBe(1);expect(Object.keys(body)).toEqual(['checkout']);expect(body.checkout.paymentMethod).toBe('CARD');await expect(page.getByText('Card billing details',{exact:true})).toHaveCount(0);await expect(page.getByText('Awaiting card confirmation',{exact:true})).toBeVisible();expect(errors).toEqual([]);
});

for(const status of ['PENDING','PARTIAL','OVERDUE']){
  test(`cashier collects ${status.toLowerCase()} invoice payments from the grid and reaches receipt printing`,async({page})=>{
    const document={...invoice,invoiceType:'SERVICE',status,amountPaid:status==='PARTIAL'?25:0,netTotal:status==='PARTIAL'?125:100};
    const errors=await setup(page,'ROLE_CASHIER',document);let body,posPayments=0;
    page.on('request',request=>{if(request.method()==='POST'&&new URL(request.url()).pathname.startsWith('/api/pos/'))posPayments++;});
    await page.route('**/api/billing/pay',async route=>{body=route.request().postDataJSON();await route.fulfill({json:{success:true,invoice:{...document,amountPaid:document.amountPaid+body.amount,balanceDue:100-body.amount,status:body.amount===100?'PAID':'PARTIAL'},paymentAmount:body.amount,paymentMethod:'CASH'}});});
    await page.goto('/dashboard/invoices');await page.getByRole('button',{name:'Pay',exact:true}).click();
    const amount=status==='PARTIAL'?50:100;
    if(status==='PARTIAL')await page.getByRole('textbox',{name:'Amount to pay (LKR)',exact:true}).fill('50');
    await page.getByRole('combobox',{name:'Payment method',exact:true}).click();await expect(page.getByRole('option',{name:/BANK|CHEQUE|OTHER/})).toHaveCount(0);await page.getByRole('option',{name:'CASH',exact:true}).click();
    await page.getByRole('button',{name:'Confirm payment',exact:true}).click();await expect.poll(()=>body).toEqual({invoiceNumber:'SP-100',amount,paymentMethod:'CASH'});await expect(page.getByText('Receipt ready',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Print receipt',exact:true})).toBeVisible();expect(posPayments).toBe(0);expect(errors).toEqual([]);
  });
}

test('local demo card checkout completes and prints a labeled preview without recording real money',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');let id,completed=false,realPayments=0;
  await page.context().addInitScript(()=>{window.print=()=>{window.printedForTest=true;};});
  await page.route('**/api/billing/pay',async route=>{realPayments++;await route.fulfill({json:{success:false}});});
  await page.route('**/api/payments/card/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith('/configuration'))return route.fulfill({json:{configured:true,mode:'DEMO'}});
    if(path.endsWith('/start')){id=route.request().headers()['idempotency-key'];return route.fulfill({json:{saleId:id,state:'DEMO_PENDING',demo:true,demoCheckout:true,amount:100}});}
    if(path.endsWith('/demo-complete'))completed=true;
    await route.fulfill({json:{saleId:id,completed,state:completed?'DEMO_COMPLETED':'DEMO_PENDING',demo:true,invoice:{...invoice,amountPaid:completed?100:0,balanceDue:completed?0:100,status:completed?'PAID':'PENDING'},paymentAmount:100,paymentMethod:'CARD',paymentDate:'2026-10-08'}});
  });
  await page.goto('/dashboard/invoices');await page.getByRole('button',{name:'Pay',exact:true}).click();await page.getByRole('combobox',{name:'Payment method',exact:true}).click();await page.getByRole('option',{name:'CARD',exact:true}).click();await page.getByRole('button',{name:'Confirm payment',exact:true}).click();
  await expect(page.getByRole('dialog',{name:'Demo card checkout',exact:true})).toBeVisible();await page.getByRole('button',{name:'Use test details',exact:true}).click();await expect(page.getByRole('textbox',{name:'Test card number'})).toHaveValue('4111 1111 1111 1111');await expect(page.getByRole('textbox',{name:'Test card number'})).not.toHaveAttribute('readonly','');
  await page.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo receipt ready',{exact:true})).toBeVisible();expect(realPayments).toBe(0);expect(await page.evaluate(()=>window.gatewayPayments?.length||0)).toBe(0);
  const opened=page.waitForEvent('popup');await page.getByRole('button',{name:'Print statement',exact:true}).click();const printed=await opened;await expect(printed.locator('h1')).toContainText('DEMO');await expect.poll(()=>printed.evaluate(()=>window.printedForTest)).toBe(true);await printed.close();
  await page.getByRole('button',{name:'Done',exact:true}).click();await expect(page.getByRole('button',{name:'Pay',exact:true})).toBeEnabled();expect(errors).toEqual([]);
});

test('POS demo card checkout reaches its receipt and print controls without loading PayHere',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');
  await page.route('**/api/pos/card/configuration',route=>route.fulfill({json:{configured:true,mode:'DEMO'}}));
  await page.route('**/api/pos/card/start',route=>route.fulfill({json:{saleId:'demo-pos-100',state:'DEMO_PENDING',demoCheckout:true,demo:true,amount:100}}));
  await page.route('**/api/pos/card/demo-pos-100/demo-complete',route=>route.fulfill({json:{completed:true,state:'DEMO_COMPLETED',demo:true,invoice:{...invoice,id:null,invoiceNumber:'DEMO-POS-100',balanceDue:0,status:'PAID'},paymentAmount:100,paymentMethod:'CARD'}}));
  await page.goto('/dashboard/pos');await page.getByRole('button',{name:'Add Engine oil 1',exact:true}).click();await page.getByRole('button',{name:'Card',exact:true}).click();const pay=page.getByRole('button',{name:'Continue to card payment',exact:true});await expect(pay).toBeEnabled();await pay.click();
  await page.getByRole('button',{name:'Use test details',exact:true}).click();await page.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo payment completed',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Print statement',exact:true})).toBeVisible();expect(await page.evaluate(()=>window.gatewayPayments?.length||0)).toBe(0);expect(errors).toEqual([]);
});

test('cancelled demo checkout can be retried from the same invoice payment form',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER'),attempts=new Map();let active;
  await page.route('**/api/payments/card/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith('/configuration'))return route.fulfill({json:{configured:true,mode:'DEMO'}});
    if(path.endsWith('/start')){
      active=route.request().headers()['idempotency-key'];expect(attempts.has(active)).toBe(false);attempts.set(active,'DEMO_PENDING');
      return route.fulfill({json:{saleId:active,state:'DEMO_PENDING',demo:true,demoCheckout:true,amount:100}});
    }
    if(path.endsWith('/cancel-unprocessed'))attempts.set(active,'CANCELLED');
    if(path.endsWith('/demo-complete'))attempts.set(active,'DEMO_COMPLETED');
    const state=attempts.get(active);
    await route.fulfill({json:{saleId:active,completed:state==='DEMO_COMPLETED',state,demo:true,invoice,paymentAmount:100,paymentMethod:'CARD'}});
  });
  await page.goto('/dashboard/invoices');await page.getByRole('button',{name:'Pay',exact:true}).click();await page.getByRole('combobox',{name:'Payment method',exact:true}).click();await page.getByRole('option',{name:'CARD',exact:true}).click();await page.getByRole('button',{name:'Confirm payment',exact:true}).click();
  await page.getByRole('dialog',{name:'Demo card checkout',exact:true}).getByRole('button',{name:'Cancel payment',exact:true}).click();
  await expect(page.getByRole('dialog',{name:'Demo card checkout',exact:true})).toBeHidden();await expect(page.getByRole('button',{name:'Confirm payment',exact:true})).toBeEnabled();
  await page.getByRole('button',{name:'Confirm payment',exact:true}).click();await page.getByRole('button',{name:'Use test details',exact:true}).click();await page.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo receipt ready',{exact:true})).toBeVisible();expect(attempts.size).toBe(2);expect(errors).toEqual([]);
});

test('unconfigured POS opens validated card details and prints a demo statement without payment API calls',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');let settlements=0;
  await page.context().addInitScript(()=>{window.print=()=>{window.printedForTest=true;};});
  await page.route('**/api/pos/card/configuration',route=>route.fulfill({json:{configured:false,sandbox:true}}));
  page.on('request',request=>{if(request.method()==='POST'&&/\/(checkout|pay|start|demo-complete)$/.test(new URL(request.url()).pathname))settlements++;});
  await page.goto('/dashboard/pos');await page.getByRole('button',{name:'Add Engine oil 1',exact:true}).click();await page.getByRole('button',{name:'Card',exact:true}).click();const pay=page.getByRole('button',{name:'Continue to card payment',exact:true});await expect(pay).toBeEnabled();await pay.click();
  const dialog=page.getByRole('dialog',{name:'Demo card checkout',exact:true});await expect(dialog).toBeVisible();await expect(page.getByText(/Card payments are not configured/)).toHaveCount(0);
  await dialog.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(dialog.getByRole('alert')).toHaveCount(4);
  await dialog.getByLabel('Cardholder name',{exact:true}).fill('  Demo123@   Customer  ');await dialog.getByLabel('Test card number',{exact:true}).fill('4111x1111!1111-1112');await dialog.getByLabel('Test expiry',{exact:true}).fill('13/30');await dialog.getByLabel('Test security code',{exact:true}).fill('1a2');
  await dialog.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(dialog.getByLabel('Cardholder name',{exact:true})).toHaveValue('Demo Customer');await expect(dialog.getByLabel('Test card number',{exact:true})).toHaveValue('4111 1111 1111 1112');await expect(dialog.getByRole('alert')).toHaveCount(3);
  await dialog.getByRole('button',{name:'Use test details',exact:true}).click();await dialog.getByLabel('Test expiry',{exact:true}).fill('01/20');await dialog.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(dialog.getByText('The card expiry must be this month or later.')).toBeVisible();
  await dialog.getByRole('button',{name:'Use test details',exact:true}).click();await dialog.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo payment completed',{exact:true})).toBeVisible();
  const opened=page.waitForEvent('popup');await page.getByRole('button',{name:'Print statement',exact:true}).click();const printed=await opened;await expect(printed.locator('h1')).toContainText('DEMO');await expect(printed.locator('body')).toContainText('Engine oil 1');await expect.poll(()=>printed.evaluate(()=>window.printedForTest)).toBe(true);await printed.close();
  expect(settlements).toBe(0);expect(await page.evaluate(()=>window.gatewayPayments?.length||0)).toBe(0);expect(await page.evaluate(()=>JSON.stringify(sessionStorage))).not.toContain('4111');expect(errors).toEqual([]);
});

test('unconfigured invoice Pay also offers a local demo and preserves the real invoice',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');let settlements=0;
  await page.route('**/api/payments/card/configuration',route=>route.fulfill({json:{configured:false}}));
  page.on('request',request=>{if(request.method()==='POST')settlements++;});
  await page.goto('/dashboard/invoices');await page.getByRole('button',{name:'Pay',exact:true}).click();await page.getByRole('combobox',{name:'Payment method',exact:true}).click();await page.getByRole('option',{name:'CARD',exact:true}).click();await page.getByRole('button',{name:'Confirm payment',exact:true}).click();
  await page.getByRole('button',{name:'Use test details',exact:true}).click();await page.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo receipt ready',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Print statement',exact:true})).toBeVisible();await page.getByRole('button',{name:'Done',exact:true}).click();await expect(page.getByRole('button',{name:'Pay',exact:true})).toBeEnabled();expect(settlements).toBe(0);expect(errors).toEqual([]);
});

test('POS payment panel and parts catalog scroll independently',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');await page.goto('/dashboard/pos');
  for(let index=1;index<=6;index++)await page.getByRole('button',{name:'Add Engine oil '+index,exact:true}).click();
  await expect(page.getByText('Updating receipt...')).toBeHidden();
  const panel=page.getByRole('complementary',{name:'POS receipt and payment'}),catalog=page.getByRole('region',{name:'Spare parts catalog'});
  await panel.evaluate(node=>node.scrollTop=0);await catalog.evaluate(node=>node.scrollTop=0);
  const windowY=await page.evaluate(()=>window.scrollY);await panel.hover();await page.mouse.wheel(0,850);await expect.poll(()=>panel.evaluate(node=>node.scrollTop)).toBeGreaterThan(0);expect(await catalog.evaluate(node=>node.scrollTop)).toBe(0);expect(await page.evaluate(()=>window.scrollY)).toBe(windowY);
  await panel.evaluate(node=>node.scrollTop=node.scrollHeight);await panel.hover();await page.mouse.wheel(0,500);expect(await catalog.evaluate(node=>node.scrollTop)).toBe(0);expect(await page.evaluate(()=>window.scrollY)).toBe(windowY);
  const panelY=await panel.evaluate(node=>node.scrollTop);await catalog.hover();await page.mouse.wheel(0,850);await expect.poll(()=>catalog.evaluate(node=>node.scrollTop)).toBeGreaterThan(0);expect(await panel.evaluate(node=>node.scrollTop)).toBe(panelY);expect(errors).toEqual([]);
});

for(const width of [1280,390]){
  test(`demo checkout is usable at ${width}px with visible details, amount and completion`,async({page},testInfo)=>{
    const errors=await setup(page,'ROLE_CASHIER');await page.setViewportSize({width,height:900});
    await page.route('**/api/pos/card/configuration',route=>route.fulfill({json:{configured:false}}));
    await page.goto('/dashboard/pos');await page.getByRole('button',{name:'Add Engine oil 1',exact:true}).click();await page.getByRole('button',{name:'Card',exact:true}).click();const pay=page.getByRole('button',{name:'Continue to card payment',exact:true});await expect(pay).toBeEnabled();await pay.click();
    const dialog=page.getByRole('dialog',{name:'Demo card checkout',exact:true});await expect(dialog).toBeVisible();await expect(dialog.getByRole('complementary',{name:'Payment summary'})).toContainText('100.00');await dialog.getByRole('button',{name:'Use test details',exact:true}).click();
    expect(await dialog.evaluate(node=>node.scrollWidth<=node.clientWidth+1)).toBe(true);await dialog.screenshot({path:testInfo.outputPath('demo-checkout.png')});
    await dialog.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo payment completed',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Print statement',exact:true})).toBeVisible();await page.getByRole('complementary',{name:'POS receipt and payment'}).screenshot({path:testInfo.outputPath('demo-completed.png')});expect(errors).toEqual([]);
  });
}

for(const role of ['ROLE_MANAGER','ROLE_ADMIN','ROLE_CASHIER']){
  test(`forbidden legacy gateway configuration opens the invoice demo for ${role}`,async({page})=>{
    const errors=await setup(page,role);let writes=0;
    await page.route('**/api/payments/card/configuration',route=>route.fulfill({status:403,json:{error:'Forbidden'}}));
    page.on('request',request=>{if(request.method()==='POST')writes++;});
    await page.goto('/dashboard/invoices');await page.getByRole('button',{name:'Pay',exact:true}).click();
    await page.getByRole('combobox',{name:'Payment method',exact:true}).click();await page.getByRole('option',{name:'CARD',exact:true}).click();await page.getByRole('button',{name:'Confirm payment',exact:true}).click();
    await expect(page.getByRole('dialog',{name:'Demo card checkout',exact:true})).toBeVisible();await expect(page.getByText('Forbidden',{exact:true})).toHaveCount(0);
    await page.getByRole('button',{name:'Use test details',exact:true}).click();await page.getByRole('button',{name:'Complete demo payment',exact:true}).click();await expect(page.getByText('Demo payment completed',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Print statement',exact:true})).toBeVisible();expect(writes).toBe(0);expect(errors).toEqual([]);
  });
}

test('settlements offer Cash and Card and one editable cash refund closes further refund actions',async({page})=>{
  const document=paidInvoice(),payment={...paidPayment},errors=await setup(page,'ROLE_MANAGER',document);let query;
  await page.route('**/api/billing/payments',route=>route.fulfill({json:{payments:[payment],totalSettled:document.amountPaid}}));
  await page.route('**/api/billing/refund-payment/10?*',async route=>{
    query=new URL(route.request().url()).searchParams;const amount=Number(query.get('amount'));
    Object.assign(document,{status:'PARTIALLY_REFUNDED',amountPaid:100-amount,refundedAmount:amount});Object.assign(payment,{status:'PARTIALLY_REFUNDED',refundedAmount:amount});await route.fulfill({json:{success:true}});
  });
  await page.goto('/dashboard/payments');await page.getByRole('button',{name:'Refund',exact:true}).click();
  await page.getByRole('combobox',{name:'Refund method',exact:true}).click();expect((await page.getByRole('listbox').getByRole('option').allTextContents()).filter(value=>['CASH','CARD'].includes(value))).toEqual(['CASH','CARD']);await expect(page.getByRole('option',{name:'QR',exact:true})).toHaveCount(0);await page.getByRole('option',{name:'CASH',exact:true}).click();
  await page.getByRole('textbox',{name:'Refund amount (LKR)',exact:true}).fill('25.50');await page.getByRole('textbox',{name:'Refund reason',exact:true}).fill('  Returned   unused item  ');await page.getByRole('button',{name:'Confirm refund',exact:true}).click();
  await expect(page.getByText('Refund completed',{exact:true})).toBeVisible();expect(query.get('paymentMethod')).toBe('CASH');expect(query.get('amount')).toBe('25.5');expect(query.get('reason')).toBe('Returned unused item');await expect(page.getByRole('button',{name:'Print statement',exact:true})).toBeVisible();await page.getByRole('button',{name:'Done',exact:true}).click();await expect(page.getByRole('button',{name:'Refund',exact:true})).toBeDisabled();expect(errors).toEqual([]);
});

test('card refund uses validated demo checkout, prints its statement and allows only one preview per invoice',async({page})=>{
  const document=paidInvoice(),errors=await setup(page,'ROLE_MANAGER',document);let writes=0;
  await page.route('**/api/billing/payments',route=>route.fulfill({json:{payments:[paidPayment]}}));
  await page.context().addInitScript(()=>{window.print=()=>{window.printedForTest=true;};});
  await page.route('**/api/payments/card/configuration',route=>route.fulfill({status:403,json:{error:'Forbidden'}}));
  page.on('request',request=>{if(request.method()==='POST')writes++;});
  await page.goto('/dashboard/invoices/1');await page.getByRole('button',{name:'Record refund',exact:true}).click();
  await page.getByRole('textbox',{name:'Refund amount (LKR)',exact:true}).fill('25.50');await page.getByRole('textbox',{name:'Refund reason',exact:true}).fill('Customer returned an unused item');await page.getByRole('combobox',{name:'Refund method',exact:true}).click();await page.getByRole('option',{name:'CARD',exact:true}).click();await page.getByRole('button',{name:'Confirm refund',exact:true}).click();
  const dialog=page.getByRole('dialog',{name:'Demo card refund',exact:true});await expect(dialog).toBeVisible();await dialog.getByRole('button',{name:'Complete demo refund',exact:true}).click();await expect(dialog.getByRole('alert')).toHaveCount(4);
  await dialog.getByRole('button',{name:'Use test details',exact:true}).click();await dialog.getByRole('button',{name:'Complete demo refund',exact:true}).click();await expect(page.getByRole('heading',{name:'Demo refund completed',exact:true})).toBeVisible();
  const opened=page.waitForEvent('popup');await page.getByRole('button',{name:'Print statement',exact:true}).click();const printed=await opened;await expect(printed.locator('h1')).toContainText('DEMO');await expect(printed.locator('h1')).toContainText('Refund statement');await expect(printed.locator('body')).toContainText('This refund');await expect(printed.locator('body')).toContainText('25.50');await expect.poll(()=>printed.evaluate(()=>window.printedForTest)).toBe(true);await printed.close();
  await page.getByRole('button',{name:'Done',exact:true}).click();await expect(page.getByRole('button',{name:'Record refund',exact:true})).toHaveCount(0);await page.reload();await expect(page.getByRole('button',{name:'Record refund',exact:true})).toHaveCount(0);await page.goto('/dashboard/payments');await expect(page.getByRole('button',{name:'Refund',exact:true})).toBeDisabled();expect(writes).toBe(0);expect(document.amountPaid).toBe(100);expect(await page.evaluate(()=>JSON.stringify(sessionStorage))).not.toContain('4111');expect(errors).toEqual([]);
});

for(const state of ['PARTIAL','PARTIALLY_REFUNDED']){
  test(`a successful payment on a ${state} invoice cannot be refunded again`,async({page})=>{
    const document={...paidInvoice(),status:state,amountPaid:50,balanceDue:state==='PARTIAL'?50:0,refundedAmount:state==='PARTIAL'?0:50};
    const errors=await setup(page,'ROLE_MANAGER',document);await page.route('**/api/billing/payments',route=>route.fulfill({json:{payments:[paidPayment]}}));
    await page.goto('/dashboard/payments');await expect(page.getByRole('button',{name:'Refund',exact:true})).toBeDisabled();expect(errors).toEqual([]);
  });
}

test('POS removes its footer and uses smaller photos across the available viewport height',async({page})=>{
  const errors=await setup(page,'ROLE_CASHIER');await page.goto('/dashboard/pos');await expect(page.getByRole('button',{name:'Add Engine oil 1',exact:true})).toBeVisible();
  await expect(page.locator('.manager-footer')).toHaveCount(0);expect((await page.locator('.cashier-product-photo').first().boundingBox()).height).toBe(132);
  const layout=await page.locator('.cashier-desk').boundingBox();expect(layout.y+layout.height).toBeGreaterThan(865);expect(layout.y+layout.height).toBeLessThanOrEqual(900);expect(await page.evaluate(()=>document.documentElement.scrollHeight)).toBeLessThanOrEqual(900);
  await page.goto('/dashboard/invoices');await expect(page.locator('.manager-footer')).toBeVisible();expect(errors).toEqual([]);
});
