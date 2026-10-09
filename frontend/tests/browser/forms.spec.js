import {test,expect} from '@playwright/test';

const vehicles=Array.from({length:18},(_,index)=>({id:index+1,licensePlate:'ABC-'+String(1000+index),make:'Toyota',model:'Corolla',ownerName:'Kasun Dias',ownerContact:'+94771234567'}));
async function mockSession(page,role='ROLE_MANAGER'){
  const failures=[];page.on('pageerror',error=>failures.push(error.message));
  await page.route('**/api/**',async route=>{
    const path=new URL(route.request().url()).pathname;
    let data={};
    if(path==='/api/auth/me'){
      if(!role)return route.fulfill({status:401,json:{message:'Sign in'}});
      data={role,user:{id:1,username:'tester',fullName:'Test User',email:'test@example.com'}};
    }else if(path==='/api/v1/notifications')data={notifications:[],unreadCount:0};
    else if(path==='/api/v1/account')data={user:{fullName:'Test User',email:'test@example.com',phoneNumber:'',address:'',city:''}};
    else if(path==='/api/v1/customer/feedback/mine'||path==='/api/v1/customer/feedback/jobs')data=[];
    else if(path==='/api/workshop/vehicles/registry')data={vehicles};
    else if(path==='/api/billing/customer-accounts')data=[];
    else if(path==='/api/pos/products')data=[];
    else if(path==='/api/workshop/bookings')data={bookings:[],vehicles};
    else if(path==='/api/services')data={services:[{id:1,name:'Full Service',active:true,estimatedCost:1000,estimatedDuration:'1 Hour'}]};
    else if(path==='/api/offers/services')data={offers:[]};
    else if(path==='/api/inventory/fuel')data={inventories:[]};
    else if(path==='/api/inventory/parts')data={parts:[]};
    else if(path==='/api/inventory/suppliers')data={suppliers:[]};
    else if(path==='/api/billing/payments')data={payments:[{id:1,invoiceNumber:'SP-123',invoiceId:1,amount:100,refundedAmount:20,status:'PARTIALLY_REFUNDED',paidOccupant:'Test Customer',paymentDate:'2026-10-08',paymentMethod:'CASH'}],totalSettled:80};
    else if(path==='/api/v1/customer/vehicles')data=vehicles;
    else if(path==='/api/v1/customer/dashboard')data={};
    else if(path==='/api/v1/customer/offers')data={offers:[]};
    else if(path==='/api/v1/customer/support')data={tickets:[],success:true};
    await route.fulfill({json:data});
  });
  return failures;
}
async function choose(page,name,option){
  await page.getByRole('combobox',{name,exact:true}).click();
  await page.getByRole('option',{name:option,exact:true}).click();
}

test('signup prevents invalid characters and normalizes phone and submitted text',async({page})=>{
  const errors=await mockSession(page,null);await page.goto('/signup');
  const name=page.locator('input[name="fullName"]');
  await expect(name).toHaveAttribute('placeholder','e.g. Kasun Dias');
  await name.pressSequentially('   Kasun@@123   Dias  ');
  await expect(name).toHaveValue('Kasun Dias ');
  await page.locator('input[name="username"]').fill('kasun.dias');
  await expect(name).toHaveValue('Kasun Dias');
  await page.locator('input[name="email"]').fill('kasun@example.com');
  const phone=page.locator('input[name="phoneNumberNational"]');
  await expect(page.getByRole('combobox',{name:/Country code for/})).toContainText('+94');
  await phone.focus();await expect(phone).toHaveValue('');
  await phone.fill('0771234567');await expect(phone).toHaveValue('771234567');
  await expect(page.locator('input[name="phoneNumber"]')).toHaveValue('+94771234567');
  const password='  a very  long passphrase  ';
  await page.locator('input[name="password"]').fill(password);await page.locator('input[name="confirmPassword"]').fill(password);
  const request=page.waitForRequest('**/api/auth/signup');await page.getByRole('button',{name:'Create account',exact:true}).click();
  const body=(await request).postDataJSON();expect(body.fullName).toBe('Kasun Dias');expect(body.phoneNumber).toBe('+94771234567');expect(body.password).toBe(password);
  expect(errors).toEqual([]);
});

test('vehicle make/model choices reset dependent values and allow uncommon vehicles',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/vehicles');
  await page.getByRole('button',{name:'Register vehicle',exact:true}).click();
  await expect(page.getByRole('combobox',{name:'Model',exact:true})).toBeDisabled();
  await choose(page,'Make','Toyota');await choose(page,'Model','Corolla');
  await choose(page,'Make','Honda');await expect(page.locator('select[name="model"]')).toHaveValue('');
  await page.getByRole('combobox',{name:'Model',exact:true}).click();
  await expect(page.getByRole('option',{name:'Corolla',exact:true})).toHaveCount(0);
  await page.keyboard.press('Escape');await expect(page.getByRole('dialog')).toBeVisible();
  await choose(page,'Model','Enter another value…');
  const model=page.locator('input[name="model"]');await model.fill('  Custom   Model@@');await model.blur();await expect(model).toHaveValue('Custom Model');
  await page.getByRole('button',{name:/Manufacture year/i}).click();
  await page.getByRole('button',{name:'2020',exact:true}).click();
  await expect(page.locator('input[name="manufactureYear"]')).toHaveValue('2020');
  expect(errors).toEqual([]);
});

test('dropdowns have compact yellow rows, keyboard selection and guest entry',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/bookings');
  const filter=page.getByRole('combobox',{name:'Filter status',exact:true});
  await filter.focus();await page.keyboard.press('ArrowDown');await expect(filter).toHaveAttribute('aria-expanded','true');
  const active=page.locator('.themed-select-option.is-active');
  expect(await active.evaluate(node=>getComputedStyle(node).backgroundColor)).toBe('rgb(230, 188, 80)');
  expect(await active.evaluate(node=>node.getBoundingClientRect().height)).toBe(32);
  await page.keyboard.press('ArrowDown');await page.keyboard.press('Enter');await expect(page.locator('select[name="query"]')).toHaveValue('Pending');
  await page.getByRole('button',{name:'New booking',exact:true}).click();
  await page.getByRole('combobox',{name:'Vehicle plate',exact:true}).click();
  const menu=page.getByRole('listbox');expect((await menu.boundingBox()).height).toBeLessThanOrEqual(194);
  const bounds=await page.getByRole('combobox',{name:'Vehicle plate',exact:true}).boundingBox();
  expect(Math.abs((await menu.boundingBox()).width-bounds.width)).toBeLessThan(2);
  await page.getByRole('option',{name:'Enter a guest vehicle plate…',exact:true}).click();
  await page.locator('input[name="licensePlate"]').fill('abc@-1234');await expect(page.locator('input[name="licensePlate"]')).toHaveValue('ABC-1234');
  expect(errors).toEqual([]);
});

test('fuel forms prevent invalid quantities and retain inline capacity validation',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/inventory/fuel');
  await page.getByRole('button',{name:'Add tank',exact:true}).click();
  await choose(page,'Fuel grade / type','Lanka Petrol 92');
  const stock=page.locator('input[name="currentStockLitres"]'),capacity=page.locator('input[name="maxCapacityLitres"]');
  await stock.fill('200.129');await expect(stock).toHaveValue('200.12');
  await capacity.fill('100');await stock.focus();await stock.blur();
  await expect(page.getByText('Stock cannot exceed storage capacity', {exact:true})).toBeVisible();
  await stock.fill('50');await stock.blur();await expect(stock).toHaveAttribute('aria-invalid','false');
  await capacity.fill('1e9');await expect(capacity).toHaveValue('100');
  expect(errors).toEqual([]);
});

test('dropdowns fit a small viewport and open above controls near the bottom',async({page})=>{
  await mockSession(page);await page.setViewportSize({width:390,height:600});await page.goto('/dashboard/vehicles');
  await page.getByRole('button',{name:'Register vehicle',exact:true}).click();
  const account=page.getByRole('combobox',{name:'Customer account',exact:true});await account.scrollIntoViewIfNeeded();await account.click();
  const menu=await page.getByRole('listbox').boundingBox();expect(menu.x).toBeGreaterThanOrEqual(0);expect(menu.x+menu.width).toBeLessThanOrEqual(390);expect(menu.y+menu.height).toBeLessThanOrEqual(600);
});

for(const role of ['ROLE_ADMIN','ROLE_MANAGER','ROLE_CASHIER','ROLE_MECHANIC','ROLE_CUSTOMER']){
  test(`${role} profile uses meaningful examples, strict names and blank optional phones`,async({page})=>{
    const errors=await mockSession(page,role);
    await page.goto(role==='ROLE_CUSTOMER'?'/customer/profile/edit':'/dashboard/profile/edit');
    const name=page.locator('input[name="fullName"]');await expect(name).toHaveValue('Test User');await name.fill('  Nimal@123   Perera ');await name.blur();await expect(name).toHaveValue('Nimal Perera');
    const phone=page.locator('input[name="phoneNumberNational"]');await expect(phone).toHaveAttribute('placeholder','e.g. 771234567');
    await expect(page.getByRole('combobox',{name:/Country code for/})).toContainText('+94');
    await phone.focus();await expect(phone).toHaveValue('');await phone.blur();await expect(phone).toHaveValue('');
    await expect(phone).toHaveAttribute('aria-invalid','false');
    await expect(page.locator('input[name="city"]')).toHaveAttribute('placeholder','e.g. Colombo');
    expect(errors).toEqual([]);
  });
}

test('customer feedback presents ratings as a compact dropdown',async({page})=>{
  const errors=await mockSession(page,'ROLE_CUSTOMER');await page.goto('/customer/feedback');
  await page.getByRole('button',{name:'Write feedback',exact:true}).click();
  const rating=page.getByRole('combobox',{name:'Rating (1?5)',exact:true});await rating.click();
  await expect(page.getByRole('option')).toHaveCount(6);
  await page.getByRole('option',{name:'4',exact:true}).click();await expect(page.locator('select[name="rating"]')).toHaveValue('4');
  expect(errors).toEqual([]);
});

test('signup accepts eight-character passwords and a country-selected international phone',async({page})=>{
  const errors=await mockSession(page,null);await page.goto('/signup');
  await page.locator('input[name="fullName"]').fill('Test Customer');await page.locator('input[name="username"]').fill('test.customer');
  const email=page.locator('input[name="email"]');await email.fill('customer@example.com');await expect(email).toHaveAttribute('maxlength','254');
  const country=page.getByRole('combobox',{name:/Country code for/});await country.click();
  await page.locator('[role="option"][title^="United Kingdom"]').click();
  const phone=page.locator('input[name="phoneNumberNational"]');await phone.fill('020 7946 0018');await phone.blur();
  await expect(page.locator('input[name="phoneNumber"]')).toHaveValue('+442079460018');await expect(phone).toHaveAttribute('aria-invalid','false');
  await page.locator('input[name="password"]').fill('Abcd123');await page.locator('input[name="confirmPassword"]').fill('Abcd123');
  await page.getByRole('button',{name:'Create account',exact:true}).click();await expect(page.getByText(/Use at least 8 characters/).first()).toBeVisible();
  await page.locator('input[name="password"]').fill('Abcd1234');await page.locator('input[name="confirmPassword"]').fill('Abcd1234');
  const request=page.waitForRequest('**/api/auth/signup');await page.getByRole('button',{name:'Create account',exact:true}).click();
  const body=(await request).postDataJSON();expect(body.password).toBe('Abcd1234');expect(body.phoneNumber).toBe('+442079460018');expect(errors).toEqual([]);
});

test('profile rejects invalid addresses and malformed email with inline feedback',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/profile/edit');
  await expect(page.locator('input[name="fullName"]')).toHaveValue('Test User');const address=page.locator('input[name="address"]');await address.fill('12345');await address.blur();
  await expect(page.getByText('Enter a meaningful address with a street or locality',{exact:true})).toBeVisible();
  await address.fill('25/3, Temple Road, Colombo');await address.blur();await expect(address).toHaveAttribute('aria-invalid','false');
  const email=page.locator('input[name="email"]');await email.fill('nametest@example');await email.blur();await expect(email).toHaveAttribute('aria-invalid','true');
  await email.fill('name+orders@example.com');await email.blur();await expect(email).toHaveAttribute('aria-invalid','false');expect(errors).toEqual([]);
});

test('supplier delivery schedules require a complete increasing time window',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/inventory/suppliers');await page.getByRole('button',{name:'Add supplier',exact:true}).click();
  await page.locator('input[name="supplierName"]').fill('Lanka Auto Supplies');await page.locator('input[name="company"]').fill('Parts Lanka');await page.locator('input[name="contactNumberNational"]').fill('0771234567');
  await choose(page,'Delivery frequency','Every Monday');await choose(page,'Delivery start time','12:00');await choose(page,'Delivery end time','09:00');
  await page.getByRole('button',{name:'Register supplier',exact:true}).click();await expect(page.getByText('Delivery end time must be after the start time on the same day',{exact:true})).toBeVisible();
  await choose(page,'Delivery end time','14:00');const request=page.waitForRequest('**/api/inventory/suppliers/add');await page.getByRole('button',{name:'Register supplier',exact:true}).click();
  const body=(await request).postDataJSON();expect(body.deliverySchedule).toBe('Every Monday, 12:00–14:00');expect(body.contactNumber).toBe('+94771234567');expect(errors).toEqual([]);
});

test('settlement refunds accept editable partial amounts and enforce the available balance',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/payments');await page.getByRole('button',{name:'Refund',exact:true}).click();
  const dialog=page.getByRole('dialog'),amount=dialog.locator('input[name="amount"]');await expect(amount).toHaveValue('80.00');
  await amount.fill('0');await amount.blur();await expect(dialog.getByText('Enter 0.01 or more',{exact:true})).toBeVisible();
  await amount.fill('80.01');await expect(amount).toHaveValue('0');
  await amount.fill('25.50');await dialog.locator('textarea[name="reason"]').fill('Customer returned one item');
  const request=page.waitForRequest('**/api/billing/refund-payment/1?*');await dialog.getByRole('button',{name:'Confirm refund',exact:true}).click();
  const sent=await request,url=new URL(sent.url());expect(url.searchParams.get('amount')).toBe('25.5');expect(sent.headers()['idempotency-key']).toBeTruthy();await expect(dialog).toHaveCount(0);expect(errors).toEqual([]);
});

test('email domains block incorrect typed and pasted characters while local parts remain editable',async({page})=>{
  const errors=await mockSession(page,null);await page.goto('/signup');
  const email=page.locator('input[name="email"]');
  await email.pressSequentially('   customer+orders@exa!#_$%mple..com   ');await expect(email).toHaveValue('customer+orders@example.com');
  await email.fill('name@company365.co.uk');await email.blur();await expect(email).toHaveValue('name@company365.co.uk');await expect(email).toHaveAttribute('aria-invalid','false');
  await email.fill('name@auto-parts.com');await email.blur();await expect(email).toHaveValue('name@auto-parts.com');await expect(email).toHaveAttribute('aria-invalid','false');
  await email.fill('name@wrong_domain.com');await expect(email).toHaveValue('name@auto-parts.com');
  await email.fill('name@example.com');await email.press('Home');await email.press('ArrowRight');await email.press('ArrowRight');await email.pressSequentially('+tag');
  await expect(email).toHaveValue('na+tagme@example.com');
  await email.fill('name@example.com');await email.press('Home');for(let i=0;i<8;i++)await email.press('ArrowRight');await email.pressSequentially('_z');await expect(email).toHaveValue('name@exazmple.com');
  expect(errors).toEqual([]);
});

test('multiline notes trim each line on blur and keep paragraph breaks',async({page})=>{
  const errors=await mockSession(page);await page.goto('/dashboard/bookings');await page.getByRole('button',{name:'New booking',exact:true}).click();
  await page.getByRole('combobox',{name:'Vehicle plate',exact:true}).click();await page.getByRole('option').filter({hasText:'ABC-1000'}).click();
  await page.getByRole('button',{name:/Full Service/}).click();await page.getByRole('button',{name:/Next step/}).click();
  const notes=page.locator('textarea[name="notes"]');
  await notes.fill('  First   line  \n  Next   line  \n\n  Last   paragraph   ');await notes.blur();
  await expect(notes).toHaveValue('First line\nNext line\n\nLast paragraph');
  await notes.fill('');await notes.pressSequentially('   First   word');await expect(notes).toHaveValue('First word');expect(errors).toEqual([]);
});

test('form submission trims notes and controlled text without needing a blur',async({page})=>{
  const errors=await mockSession(page,'ROLE_CUSTOMER');await page.goto('/customer/support');await page.getByRole('button',{name:/new ticket/i}).click();
  const subject=page.locator('input[name="subject"]'),message=page.locator('textarea[name="message"]');
  await subject.fill('  Service   request   ');await message.fill('  First   line  \n  Second   line   ');
  const request=page.waitForRequest('**/api/v1/customer/support');
  await message.evaluate(node=>{node.form.addEventListener('submit',()=>{window.notesBeforeSubmit=node.value;},{once:true});node.form.requestSubmit();});
  const body=(await request).postDataJSON();expect(body.subject).toBe('Service request');expect(body.message).toBe('First line\nSecond line');expect(await page.evaluate(()=>window.notesBeforeSubmit)).toBe('First line\nSecond line');expect(errors).toEqual([]);
});
