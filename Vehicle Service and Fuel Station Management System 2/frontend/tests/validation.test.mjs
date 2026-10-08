import test from 'node:test';
import assert from 'node:assert/strict';
import {normalizePayload,normalizePhone,normalizeText,placeholderFor,ruleFor,sanitizeInput,validateFields,validateValue} from '../src/lib/validation.js';
import {billingCountries,vehicleModels} from '../src/lib/fieldOptions.js';
import {countryForPhone,normalizeNational,tooLongPhone} from '../src/lib/phoneNumbers.js';
import {normalizeDeliverySchedule} from '../src/lib/deliverySchedule.js';

test('names prevent digits, punctuation and repeated spaces, and support Unicode',()=>{
  const rule=ruleFor('fullName','Full name');
  assert.equal(sanitizeInput('  Kasun@123   Dias  ',rule),'Kasun Dias ');
  assert.equal(sanitizeInput('  Kasun   Dias  ',rule,{final:true}),'Kasun Dias');
  assert.equal(validateValue('José Silva',rule),'');
  assert.ok(validateValue('Kasun@123',rule));
  assert.ok(validateValue('Kasun  Dias',rule));
});
test('identifiers, email and plates admit their relevant characters',()=>{
  assert.equal(sanitizeInput(' user name!?',ruleFor('username')),'username');
  assert.equal(sanitizeInput(' kasun @example.com ',ruleFor('email')),'kasun@example.com');
  assert.equal(sanitizeInput(' wp  caa@-1234 ',ruleFor('licensePlate')),'WP CAA-1234 ');
  assert.equal(validateValue('WP CAA-1234',ruleFor('licensePlate')),'');
});
test('phones include an international prefix, including legacy Sri Lankan input',()=>{
  for(const input of ['0771234567','771234567','+94 77 123 4567','0094771234567'])assert.equal(normalizePhone(input),'+94771234567');
  assert.equal(sanitizeInput('+940771234567',ruleFor('phone')),'+94771234567');
  assert.equal(sanitizeInput('+94',ruleFor('phone'),{final:true}),'');
  assert.equal(validateValue('+94771234567',ruleFor('phone')),'');
  assert.equal(validateValue('+14155552671',ruleFor('phone')),'');
  for(const input of ['0771234567','+940771234567','+9477123456','+1234567890123456'])assert.ok(validateValue(input,ruleFor('phone')));
});
test('numeric fields reject exponents, negative values, fractions and excessive values',()=>{
  const count=ruleFor('quantity');
  for(const input of ['1e3','-2','2.5','10000'])assert.equal(sanitizeInput(input,count,{previous:'2'}),'2');
  assert.equal(sanitizeInput('125.678',ruleFor('amount')),'125.67');
  assert.ok(validateValue('0',count));
  assert.ok(validateValue('125.678',ruleFor('amount')));
});
test('normalization preserves paragraphs, password whitespace and security tokens',()=>{
  const password='  a very  long passphrase  ';
  const data=normalizePayload({fullName:'  Kasun   Dias  ',phoneNumber:'0771234567',notes:' First   line  \n  Next   line ',password,token:'  exact  token ',items:[{partName:' Oil   filter '}]});
  assert.equal(data.fullName,'Kasun Dias');assert.equal(data.phoneNumber,'+94771234567');
  assert.equal(data.notes,'First line\nNext line');assert.equal(data.password,password);assert.equal(data.token,'  exact  token ');
  assert.equal(data.items[0].partName,'Oil filter');
  assert.equal(sanitizeInput(password,ruleFor('password'),{final:true}),password);
  assert.equal(sanitizeInput('é'.repeat(37),ruleFor('password'),{previous:password}),password);
});
test('placeholders provide examples without duplicating labels or saying optional',()=>{
  for(const [key,label] of [['fullName','Full name'],['phoneNumber','Phone number'],['email','Email'],['city','City'],['address','Address'],['notes','Notes']]){
    for(const provided of [undefined,label,'optional']){
      const placeholder=placeholderFor(ruleFor(key,label),provided);
      assert.notEqual(placeholder,label);assert.ok(!/optional/i.test(placeholder));assert.ok(placeholder.length>5);
    }
  }
});
test('dependent model choices preserve existing vehicles and allow custom models',()=>{
  assert.ok(vehicleModels('Toyota').includes('Corolla'));
  assert.ok(!vehicleModels('Honda').includes('Corolla'));
  assert.ok(vehicleModels('Unknown make','Existing model').includes('Existing model'));
  assert.deepEqual(validateFields([{key:'make',label:'Make',options:()=>['Toyota'],allowCustom:true}],{make:'Custom Motors'}),{});
});
test('every billing country is valid under location rules',()=>{
  for(const country of billingCountries)assert.equal(validateValue(country,ruleFor('country')),'',country);
});
test('email boundaries and malformed local/domain parts are checked',()=>{
  const rule=ruleFor('email');
  const longest='a'.repeat(64)+'@'+'b'.repeat(63)+'.'+'c'.repeat(63)+'.'+'d'.repeat(57)+'.com';
  assert.equal(longest.length,254);assert.equal(validateValue(longest,rule),'');
  for(const input of ['a'.repeat(65)+'@example.com','name@'+'a'.repeat(64)+'.com','.name@example.com','name.@example.com','na..me@example.com','name@@example.com','name@-example.com','name@example-.com','name@example','name@exam_ple.com',longest+'a'])assert.ok(validateValue(input,rule),input);
  assert.equal(validateValue('customer+orders@example.co.uk',rule),'');
  assert.equal(sanitizeInput('a'.repeat(65),rule,{previous:'a'.repeat(64)}),'a'.repeat(64));
  assert.equal(sanitizeInput('name@@example.com',rule,{previous:'name@example.com'}),'name@example.com');
});
test('phone validation checks actual national numbering patterns and limits',()=>{
  for(const input of ['+94771234567','+94112345678','+14155552671','+442079460018','+919876543210'])assert.equal(validateValue(input,ruleFor('phone')),'',input);
  for(const input of ['+94121234567','+940771234567','+1415555','+999123456789','+947712345678'])assert.ok(validateValue(input,ruleFor('phone')),input);
  assert.equal(countryForPhone(''),'LK');assert.equal(countryForPhone('+442079460018'),'GB');
  assert.equal(normalizeNational('077 123 4567','LK'),'771234567');
  assert.equal(normalizeNational('020 7946 0018','GB'),'2079460018');
  assert.ok(tooLongPhone('7712345678','LK'));
});
test('new passwords accept eight characters but retain the byte limit and confirmation checks',()=>{
  const rule=ruleFor('newPassword','New password',{newPassword:true});
  assert.equal(validateValue('Abcd1234',rule),'');assert.ok(validateValue('Abcd123',rule));
  assert.equal(validateValue('é'.repeat(8),rule),'');assert.ok(validateValue('é'.repeat(37),rule));
  assert.equal(validateValue('short',ruleFor('currentPassword')),'');
  assert.ok(validateValue('Abcd1234',rule,{currentPassword:'Abcd1234'}));
});
test('addresses accept real localities and reject short, numeric-only and punctuation-only entries',()=>{
  const rule=ruleFor('address');
  for(const value of ['25/3, Temple Road, Colombo','Colombo','12 ශ්‍රී මාවත','Apartment #4, St. John’s Road'])assert.equal(validateValue(value,rule),'',value);
  for(const value of ['A','12345','.....','Road !!!','Temple... Road','a'.repeat(256)])assert.ok(validateValue(value,rule),value);
  assert.equal(sanitizeInput('  25   Temple Road@@  ',rule,{final:true}),'25 Temple Road');
});
test('delivery windows require a supported frequency and real, increasing times',()=>{
  const rule=ruleFor('deliverySchedule');
  for(const value of ['Daily, 09:00–12:00','Every Monday, 9 am to noon','Weekdays, 00:00-23:59'])assert.equal(validateValue(value,rule),'',value);
  for(const value of ['Soon','Daily, 25:00–26:00','Daily, 12:00–12:00','Weekends, 18:00–09:00','Every Funday, 09:00–12:00','Daily, –'])assert.ok(validateValue(value,rule),value);
  assert.equal(normalizeDeliverySchedule('Every Monday, 9 am to noon'),'Every Monday, 09:00–12:00');
  assert.equal(validateValue('',rule),'');
});
test('refund amounts have cent precision and cannot exceed the remaining balance',()=>{
  const rule=ruleFor('amount','Refund amount',{required:true,min:.01,max:79.99});
  for(const value of ['0.01','20.50','79.99'])assert.equal(validateValue(value,rule),'');
  for(const value of ['0','-1','80','0.001','Infinity','1e1',''])assert.ok(validateValue(value,rule),value);
});
test('email typing blocks domain punctuation and incorrect separators without changing the accepted address',()=>{
  const rule=ruleFor('email');
  let value='';
  for(const character of 'customer+orders@exa!#_$%mple..com')value=sanitizeInput(value+character,rule,{previous:value});
  assert.equal(value,'customer+orders@example.com');
  for(const character of "!#$%&'*+/=?^_`{|}~@")assert.equal(sanitizeInput('name@example'+character,rule,{previous:'name@example'}),'name@example');
  for(const input of ['name@-example.com','name@.example.com','name@example-.com','name@example..com','name@new_domain.com'])assert.equal(sanitizeInput(input,rule,{previous:'name@example.com'}),'name@example.com');
  assert.equal(sanitizeInput('name@company365.co.uk',rule),'name@company365.co.uk');
  assert.equal(sanitizeInput('name@auto-parts.com',rule),'name@auto-parts.com');
  assert.equal(validateValue('name@company365.co.uk',rule),'');
  assert.equal(validateValue('name@auto-parts.com',rule),'');
  assert.equal(sanitizeInput('na+tagme@example.com',rule),'na+tagme@example.com');
  assert.equal(sanitizeInput('name@example_',ruleFor('identifier'),{previous:'name@example'}),'name@example');
});
test('all ordinary text fields trim on finalization and notes retain their paragraphs',()=>{
  for(const key of ['fullName','username','email','address','notes','comments','message','reason','subject','title','query']){
    const input=key==='email'?' \t name@example.com \u00a0 ':key==='username'?' \t test.customer \u00a0 ':' \t Temple   Road \u00a0 ';
    const expected=key==='email'?'name@example.com':key==='username'?'test.customer':'Temple Road';
    assert.equal(sanitizeInput(input,ruleFor(key),{final:true}),expected,key);
  }
  const raw=' \t First   line  \r\n \t Next   line \t \r\n\r\n  Last   paragraph  ';
  const expected='First line\nNext line\n\nLast paragraph';
  assert.equal(normalizeText(raw,{multiline:true}),expected);
  assert.equal(sanitizeInput(raw,ruleFor('notes','Notes',{multiline:true}),{final:true}),expected);
  assert.equal(normalizePayload({notes:raw,email:'  name@example.com \t'}).notes,expected);
});
