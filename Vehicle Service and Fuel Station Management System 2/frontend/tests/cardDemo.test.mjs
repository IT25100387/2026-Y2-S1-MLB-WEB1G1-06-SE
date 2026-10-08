import test from 'node:test';
import assert from 'node:assert/strict';
import {sanitizeDemoCard,testCardDetails,validateDemoCard,demoReceiptFromQuote,demoReceiptFromInvoice,localCardDemoAllowed} from '../src/lib/cardDemo.js';

const date=new Date('2026-10-08T06:00:00Z'),card=testCardDetails(date);
test('demo fields filter unrelated characters, limit lengths and normalize names',()=>{
  assert.equal(sanitizeDemoCard('cardHolder','  Kasun@123   Dias  ',true),'Kasun Dias');
  assert.equal(sanitizeDemoCard('cardNumber','4111a! 1111-1111.111199'),'4111 1111 1111 1111');
  assert.equal(sanitizeDemoCard('expiry','12x/30 99'),'12/30');
  assert.equal(sanitizeDemoCard('securityCode','a1b2c345'),'123');
});
test('card validation rejects incomplete details and invalid check digits or other cards',()=>{
  assert.deepEqual(validateDemoCard(card,date),{});
  assert.equal(Object.keys(validateDemoCard({},date)).length,4);
  assert.ok(validateDemoCard({...card,cardNumber:'4111 1111 1111 1112'},date).cardNumber);
  assert.ok(validateDemoCard({...card,cardNumber:'4242 4242 4242 4242'},date).cardNumber);
  assert.ok(validateDemoCard({...card,securityCode:'12'},date).securityCode);
});
test('expiry validates month boundaries using Sri Lanka time',()=>{
  assert.ok(validateDemoCard({...card,expiry:'00/30'},date).expiry);
  assert.ok(validateDemoCard({...card,expiry:'13/30'},date).expiry);
  assert.ok(validateDemoCard({...card,expiry:'09/26'},date).expiry);
  assert.deepEqual(validateDemoCard({...card,expiry:'10/26'},date),{});
  assert.ok(validateDemoCard({...card,expiry:'12/99'},date).expiry);
  assert.ok(validateDemoCard({...card,expiry:'09/26'},new Date('2026-09-30T20:00:00Z')).expiry);
});
test('local fallback stays unavailable on public hosts and when a real gateway is configured',()=>{
  for(const host of ['localhost','127.0.0.1','[::1]'])assert.equal(localCardDemoAllowed({configured:false},host),true);
  assert.equal(localCardDemoAllowed({configured:false},'station.example'),false);
  assert.equal(localCardDemoAllowed({configured:true},'localhost'),false);
});
test('demo statement previews a partial payment without mutating the real invoice',()=>{
  const invoice={id:1,invoiceNumber:'SV-100',invoiceType:'SERVICE',finalized:true,netTotal:100,amountPaid:25,balanceDue:75,status:'PARTIAL',lineItems:[{itemName:'Service',quantity:1,unitPrice:100,totalAmount:100}]};
  const before=structuredClone(invoice),receipt=demoReceiptFromInvoice(invoice,50,'demo-100');
  assert.equal(receipt.demo,true);assert.equal(receipt.invoice.balanceDue,25);assert.equal(receipt.invoice.amountPaid,75);
  receipt.invoice.lineItems[0].itemName='Preview';assert.deepEqual(invoice,before);
  assert.throws(()=>demoReceiptFromInvoice(invoice,75.01,'demo-100'));
  assert.throws(()=>demoReceiptFromInvoice({...invoice,finalized:false},75,'demo-100'));
  assert.throws(()=>demoReceiptFromInvoice({...invoice,status:'VOID'},50,'demo-100'));
});
test('parts demo statements require full payment and contain item totals and Sri Lanka date',()=>{
  const quote={total:2500,balance:2500,finalized:true,lines:[{itemName:'Engine oil',quantity:2,unitPrice:1250}]};
  const receipt=demoReceiptFromQuote(quote,2500,'checkout-100',new Date('2026-10-07T20:00:00Z'));
  assert.equal(receipt.paymentDate,'2026-10-08');assert.equal(receipt.invoice.dueDate,'2026-10-08');assert.equal(receipt.invoice.lineItems[0].totalAmount,2500);assert.equal(receipt.invoice.id,null);
  assert.throws(()=>demoReceiptFromQuote(quote,100,'checkout-100'));
  assert.throws(()=>demoReceiptFromQuote(quote,Number.NaN,'checkout-100'));
});
