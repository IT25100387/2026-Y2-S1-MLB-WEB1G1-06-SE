// Shared field rules. The server repeats these checks; client checks are feedback only.
export const stationDate=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export const utf8Length=value=>new TextEncoder().encode(String(value)).length;
const humanName=/^[\p{L}\p{M}][\p{L}\p{M} .’'\-]*$/u;
const username=/^[A-Za-z0-9][A-Za-z0-9._-]{2,39}$/;
const email=/^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;
const phone=value=>{
  if(!/^\+?[\d ()-]+$/.test(value))return false;
  const clean=value.replace(/[ ()-]/g,'');
  return clean.startsWith('+94')?/^\+94[1-9]\d{8}$/.test(clean):clean.startsWith('+')?/^\+[1-9]\d{7,14}$/.test(clean):/^0[1-9]\d{8}$/.test(clean)||/^[1-9]\d{8}$/.test(clean);
};
const aliases={
  'full name':'fullName','first name':'firstName','last name':'lastName','employee name':'staffName','guest customer name':'customerName','registered owner':'ownerName','owner name':'ownerName',
  'username':'username','username or email':'identifier','email':'email','phone':'phone','phone number':'phoneNumber','guest contact':'customerPhone','contact':'ownerContact','owner contact':'ownerContact','contact number':'contactNumber',
  'vehicle plate':'licensePlate','registration plate':'licensePlate','service reference':'referenceNumber','country':'country','city':'city','billing address':'address','address':'address',
  'new date':'newDate','select date':'serviceDate','service date':'serviceDate','start date':'startDate','end date':'endDate','shift date':'shiftDate','manufacture year':'manufactureYear',
  'mechanic notes':'mechanicNotes','problem description / notes':'notes','notes':'notes','comments':'comments','message':'message','reply':'adminReply','reason':'reason','refund reason':'reason',
  'quantity':'quantity','used quantity':'quantity','rating (1?5)':'rating','stock quantity':'stockQuantity','min stock warning':'minStockWarning','reason for deactivation':'deactivationReason',
  'current stock (litres)':'currentStockLitres','max storage capacity (litres)':'maxCapacityLitres','min stock warning level (litres)':'minStockWarning',
  'fuel liters sold today':'litersSold','actual cash collected (lkr)':'collectedAmount','amount (lkr)':'amount','payment amount (rs.)':'amount','cash received (rs.)':'cashTendered',
  'buying price (rs)':'buyingPrice','selling price (rs)':'sellingPrice','fuel grade / type':'fuelType','part name':'partName','supplier name':'supplierName','company':'company','delivery schedule':'deliverySchedule',
};
export function inferKey(label=''){
  const clean=String(label).replace(/\s*\*\s*$/,'').trim().toLowerCase();
  if(aliases[clean])return aliases[clean];
  if(/search|filter/.test(clean))return 'query';
  if(/^quantity for /.test(clean))return 'quantity';
  return clean.replace(/[^a-z0-9]+(.)?/g,(_,letter)=>letter?letter.toUpperCase():'');
}
export function ruleFor(key='',label='',overrides={}){
  key=key||inferKey(label);
  let rule={key,label:label||key,kind:'text',maxLength:255};
  if(/query|search|filter/i.test(key)||/search|filter/i.test(label))rule={...rule,kind:'search',maxLength:200};
  else if(/^(fullName|firstName|lastName|ownerName|customerName|staffName)$/.test(key))rule={...rule,kind:'name',minLength:2,maxLength:100};
  else if(/^(username|systemUsername|ownerUsername|customerUsername)$/.test(key))rule={...rule,kind:'username',minLength:3,maxLength:40};
  else if(key==='identifier')rule={...rule,kind:'identifier',maxLength:254};
  else if(/email/i.test(key))rule={...rule,kind:'email',maxLength:254};
  else if(/phone|contactNumber|ownerContact/i.test(key))rule={...rule,kind:'phone',maxLength:25};
  else if(/password/i.test(key))rule={...rule,kind:'password',maxLength:72};
  else if(key==='licensePlate'||key==='vehicleNumber')rule={...rule,kind:'plate',maxLength:20};
  else if(key==='referenceNumber')rule={...rule,kind:'reference',maxLength:64};
  else if(key==='invoiceNumber')rule={...rule,kind:'invoice',maxLength:64};
  else if(/Date$|^(date|issueFrom|issueTo|dueFrom|dueTo|from|to|day)$/.test(key)||overrides.type==='date')rule={...rule,kind:'date',min:'1900-01-01',max:'2100-12-31'};
  else if(/^(quantity|stockQuantity|minStockWarning|mileage|manufactureYear|rating)$/.test(key))rule={...rule,kind:'number',integer:true,min:key==='quantity'||key==='rating'?1:0,max:key==='quantity'?9999:key==='rating'?5:2147483647};
  else if(/amount|price|cost|litres|liters|capacity|Reading|discountPercentage|durationHours/i.test(key)||overrides.type==='number')rule={...rule,kind:'number',min:0,max:999999999.99,decimals:2};
  if(/^(mechanicNotes|problemDescription|message|adminReply|shortDescription)$/.test(key))rule={...rule,maxLength:4000,multiline:true};
  if(key==='name'&&/employee|staff/i.test(label))rule={...rule,kind:'name',minLength:2,maxLength:60};
  if(/^(reason|refundReason|deactivationReason|deliverySchedule)$/.test(key))rule={...rule,minLength:3,maxLength:255,multiline:true};
  if(key==='city'||key==='country')rule={...rule,minLength:2,maxLength:80};
  if(/Name$|^(name|make|model|category|company|fuelType|bayName|pumpName|station|assignedStation|role|subject)$/.test(key)&&rule.kind==='text')rule={...rule,minLength:2,maxLength:key==='subject'?200:100};
  if(key==='manufactureYear')rule={...rule,min:1900,max:Number(stationDate().slice(0,4))+1};
  if(key==='rating')rule={...rule,min:1,max:5,integer:true};
  if(key==='discountPercentage')rule={...rule,min:0,max:100};
  if(key==='durationHours')rule={...rule,min:.25,max:24,step:.25};
  if(key==='maxCapacityLitres')rule={...rule,min:.01};
  if(key==='minStockWarning'&&/litres|liters/i.test(label))rule={...rule,integer:false,decimals:2,max:999999999.99};
  if(['serviceDate','newDate','shiftDate','startDate'].includes(key))rule={...rule,min:stationDate()};
  if(key==='registeredDate')rule={...rule,max:stationDate()};
  if(/^(engineNumber|chassisNumber)$/.test(key))rule={...rule,kind:'identifierCode',maxLength:50};
  if(key==='estimatedDuration')rule={...rule,kind:'duration',maxLength:40};
  if(overrides.type==='number')rule.kind='number';
  return {...rule,...overrides,key};
}
export function validateValue(value,rule,form={}){
  const {kind,label='This field'}=rule,raw=String(value??''),clean=kind==='password'?raw:raw.trim();
  if(!clean.trim())return rule.required?`${label} is required`:'';
  if(rule.options){const options=rule.options.map(item=>String(typeof item==='object'?item.value:item));if(!options.includes(raw))return 'Choose an available option';}
  if(kind==='password'){
    if(utf8Length(raw)>72)return 'Use at most 72 UTF-8 bytes for this password';
    if(/[\u0000-\u001f\u007f]/.test(raw))return 'Remove control characters from the password';
    if(rule.newPassword&&[...raw].length<15)return 'Use at least 15 characters; a longer passphrase works well';
    if(rule.key==='confirmPassword'&&raw!==(form.newPassword??form.password))return 'Passwords do not match';
    if(rule.newPassword&&form.currentPassword&&raw===form.currentPassword)return 'Choose a password different from your current password';
    return '';
  }
  if(kind==='number'){
    if(!/^-?(?:\d+(?:\.\d*)?|\.\d+)$/.test(clean)||!Number.isFinite(Number(clean)))return rule.integer?'Enter a whole number':'Enter a valid number without exponent notation';
    const n=Number(clean);
    if(rule.integer&&!Number.isSafeInteger(n))return 'Enter a whole number';
    if(rule.min!=null&&n<Number(rule.min))return `Enter ${rule.min} or more`;
    if(rule.max!=null&&n>Number(rule.max))return `Enter ${rule.max} or less`;
    if(!rule.integer&&(clean.split('.')[1]?.length||0)>(rule.decimals??2))return `Use at most ${rule.decimals??2} decimal places`;
    if(rule.step&&Math.abs(n/Number(rule.step)-Math.round(n/Number(rule.step)))>1e-8)return `Use increments of ${rule.step}`;
    return '';
  }
  if(rule.maxLength&&raw.length>rule.maxLength)return `Use at most ${rule.maxLength} characters`;
  if(rule.minLength&&[...clean].length<rule.minLength)return `Use at least ${rule.minLength} characters`;
  if((rule.multiline?/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/:/[\u0000-\u001f\u007f]/).test(raw))return 'Remove control characters from this value';
  if(kind==='name'&&!humanName.test(clean))return 'Use letters, spaces, apostrophes, dots or hyphens for the name';
  if(kind==='username'&&!username.test(clean))return 'Use 3–40 letters/numbers, dots, underscores or hyphens; start with a letter/number';
  if(kind==='email'&&(!email.test(clean)||clean.split('@')[0].length>64||clean.startsWith('.')||clean.split('@')[0].endsWith('.')||clean.split('@')[0].includes('..')))return 'Enter a valid email address, such as name@example.com';
  if(kind==='phone'&&!phone(clean))return 'Enter a valid phone, e.g. 0771234567 or +94 77 123 4567';
  if(kind==='identifier'&&validateValue(clean,ruleFor(clean.includes('@')?'email':'username',label)))return 'Enter a valid username or email address';
  if(kind==='plate'&&!/^(?:(?:WP|CP|SP|NP|EP|NW|NC|UP|SG)\s+)?(?:[A-Z]{1,3}|\d{1,3})[ -]?\d{4}$/i.test(clean))return 'Use a valid plate, e.g. ABC-1234 or WP CAA-1234';
  if(kind==='reference'&&!/^[A-Za-z0-9][A-Za-z0-9-]{2,63}$/.test(clean))return 'Use 3–64 letters, numbers or hyphens for the service reference';
  if(kind==='invoice'&&!/^(?:INV|SP|SRV|FUEL)-[A-Za-z0-9-]{1,60}$/i.test(clean))return 'Enter a valid invoice number, e.g. SP-123 or SRV-123';
  if(kind==='identifierCode'&&!/^[A-Za-z0-9][A-Za-z0-9 /-]*$/.test(clean))return 'Use letters, numbers, spaces, slashes or hyphens';
  if(kind==='duration'){
    if(!/^(?:\d+(?:\.\d+)?\s*(?:hours?|hrs?|h|minutes?|mins?|m)\s*)+$/i.test(clean))return 'Enter a duration such as 1 Hour 30 Minutes';
    const minutes=[...clean.matchAll(/(\d+(?:\.\d+)?)\s*(hours?|hrs?|h|minutes?|mins?|m)/gi)].reduce((sum,m)=>sum+Number(m[1])*(m[2].toLowerCase().startsWith('h')?60:1),0);
    if(minutes<1||minutes>540)return 'Use a duration from 1 minute to 9 hours';
  }
  if(kind==='date'){
    const date=new Date(clean+'T00:00:00Z');
    if(!/^\d{4}-\d{2}-\d{2}$/.test(clean)||!Number.isFinite(date.getTime())||date.toISOString().slice(0,10)!==clean)return 'Enter a real date';
    if(rule.min&&clean<rule.min)return `Choose ${rule.min} or later`;
    if(rule.max&&clean>rule.max)return `Choose ${rule.max} or earlier`;
  }
  if(rule.validate){const issue=rule.validate(value,form);if(issue)return issue;}
  return '';
}
export function relatedErrors(values){
  const issues={};
  for(const [start,end] of [['startDate','endDate'],['issueFrom','issueTo'],['dueFrom','dueTo'],['from','to']])if(values[start]&&values[end]&&values[end]<values[start])issues[end]='End date must be on or after the start date';
  const has=key=>values[key]!==undefined&&values[key]!==null&&values[key]!=='';
  if(has('currentStockLitres')&&has('maxCapacityLitres')&&Number(values.currentStockLitres)>Number(values.maxCapacityLitres))issues.currentStockLitres='Stock cannot exceed storage capacity';
  if(has('minStockWarning')&&has('maxCapacityLitres')&&Number(values.minStockWarning)>Number(values.maxCapacityLitres))issues.minStockWarning='Warning level cannot exceed storage capacity';
  if(values.confirmPassword!==undefined&&values.confirmPassword!==(values.newPassword??values.password))issues.confirmPassword='Passwords do not match';
  if(values.newPassword&&values.currentPassword&&values.newPassword===values.currentPassword)issues.newPassword='Choose a password different from your current password';
  return issues;
}
export function validateFields(fields,values){
  const issues=relatedErrors(values);
  for(const field of fields){const issue=validateValue(values[field.key],ruleFor(field.key,field.label,{...field,required:!field.optional,newPassword:field.newPassword??(field.type==='password'&&field.key!=='currentPassword')}),values);if(issue)issues[field.key]=issue;}
  return issues;
}
