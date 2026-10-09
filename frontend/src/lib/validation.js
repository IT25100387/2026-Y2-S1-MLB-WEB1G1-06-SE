import {validPhone} from './phoneNumbers.js';
import {deliveryScheduleError,normalizeDeliverySchedule} from './deliverySchedule.js';
// Shared field rules. The server repeats these checks; client checks are feedback only.
export const stationDate=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export const utf8Length=value=>new TextEncoder().encode(String(value)).length;
const humanName=/^[\p{L}\p{M}]+(?: [\p{L}\p{M}]+)*$/u;
const username=/^[A-Za-z0-9][A-Za-z0-9._-]{2,39}$/;
const email=/^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/;
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
  else if(/^(fullName|firstName|lastName|ownerName|customerName|staffName|assignedOperator)$/.test(key))rule={...rule,kind:'name',minLength:2,maxLength:100};
  else if(/^(username|systemUsername|ownerUsername|customerUsername)$/.test(key))rule={...rule,kind:'username',minLength:3,maxLength:40};
  else if(key==='identifier')rule={...rule,kind:'identifier',maxLength:254};
  else if(/email/i.test(key))rule={...rule,kind:'email',maxLength:254};
  else if(/phone|contactNumber|ownerContact/i.test(key))rule={...rule,kind:'phone',maxLength:16};
  else if(/password/i.test(key))rule={...rule,kind:'password',maxLength:72};
  else if(key==='licensePlate'||key==='vehicleNumber')rule={...rule,kind:'plate',maxLength:20};
  else if(key==='referenceNumber')rule={...rule,kind:'reference',maxLength:64};
  else if(key==='invoiceNumber')rule={...rule,kind:'invoice',maxLength:64};
  else if(/Date$|^(date|issueFrom|issueTo|dueFrom|dueTo|from|to|day)$/.test(key)||overrides.type==='date')rule={...rule,kind:'date',min:'1900-01-01',max:'2100-12-31'};
  else if(/^(quantity|stockQuantity|minStockWarning|mileage|rating)$/.test(key))rule={...rule,kind:'number',integer:true,min:key==='quantity'||key==='rating'?1:0,max:key==='quantity'?9999:key==='rating'?5:2147483647};
  else if(/amount|price|cost|litres|liters|capacity|Reading|discountPercentage|durationHours/i.test(key)||overrides.type==='number')rule={...rule,kind:'number',min:0,max:999999999.99,decimals:2};
  if(/^(mechanicNotes|problemDescription|message|adminReply|shortDescription)$/.test(key))rule={...rule,maxLength:4000,multiline:true};
  if(key==='name'&&/employee|staff/i.test(label))rule={...rule,kind:'name',minLength:2,maxLength:60};
  if(/^(reason|refundReason|deactivationReason)$/.test(key))rule={...rule,minLength:3,maxLength:255,multiline:true};
  if(key==='deliverySchedule')rule={...rule,kind:'schedule',maxLength:255};
  if(key==='city'||key==='country')rule={...rule,kind:key==='country'?'catalog':'place',minLength:2,maxLength:80};
  if(/Name$|^(name|make|model|category|company|fuelType|bayName|pumpName|station|assignedStation|role|subject)$/.test(key)&&rule.kind==='text')rule={...rule,minLength:2,maxLength:key==='subject'?200:100};
  if(/^(name|make|model|category|company|fuelType|bayName|pumpName|station|assignedStation|partName|goodsItem|role)$/.test(key)&&rule.kind==='text')rule.kind='catalog';
  if(key==='supplierName')rule={...rule,kind:'catalog',minLength:2,maxLength:100};
  if(key==='manufactureYear'||overrides.type==='year'||/year$/i.test(key))rule={...rule,kind:'year',integer:true,min:overrides.min??1900,max:overrides.max??(Number(stationDate().slice(0,4))+1)};
  if(key==='rating')rule={...rule,min:1,max:5,integer:true};
  if(key==='discountPercentage')rule={...rule,min:0,max:100};
  if(key==='durationHours')rule={...rule,min:.25,max:24,step:.25};
  if(key==='maxCapacityLitres')rule={...rule,min:.01};
  if(key==='minStockWarning'&&/litres|liters/i.test(label))rule={...rule,integer:false,decimals:2,max:999999999.99};
  if(['serviceDate','newDate','shiftDate','startDate'].includes(key))rule={...rule,min:stationDate()};
  if(key==='registeredDate')rule={...rule,max:stationDate()};
  if(/^(engineNumber|chassisNumber)$/.test(key))rule={...rule,kind:'identifierCode',maxLength:50};
  if(key==='pumpNumber')rule={...rule,kind:'identifierCode',maxLength:20};
  if(key==='address'||key==='location')rule={...rule,kind:'address',minLength:key==='address'?5:2};
  if(key==='title')rule.maxLength=150;
  if(key==='estimatedDuration')rule={...rule,kind:'duration',maxLength:40};
  if(overrides.type==='number')rule.kind='number';
  return {...rule,...overrides,key};
}

const examples={
  fullName:'e.g. Kasun Dias',firstName:'e.g. Kasun',lastName:'e.g. Dias',ownerName:'e.g. Nuwan Kulasekara',customerName:'e.g. Kasun Dias',staffName:'e.g. Nimal Perera',
  username:'e.g. kasun.dias',systemUsername:'e.g. nimal.perera',ownerUsername:'Choose a customer account',identifier:'e.g. kasun.dias or kasun@example.com',
  email:'e.g. kasun@example.com',licensePlate:'e.g. ABC-1234',vehicleNumber:'e.g. ABC-1234',referenceNumber:'e.g. SRV-12345',invoiceNumber:'e.g. SP-123',
  make:'Choose a manufacturer',model:'Choose a model for this make',manufactureYear:'e.g. 2020',mileage:'e.g. 45000',engineNumber:'e.g. 1NZ-1234567',chassisNumber:'e.g. JTDBR32E123456789',
  address:'e.g. 25 Temple Road, Colombo',city:'e.g. Colombo',country:'Choose your billing country',
  notes:'Describe symptoms or requests for this visit',problemDescription:'e.g. Brakes squeak when slowing down',mechanicNotes:'Record the work completed and any findings',
  message:'Describe your request with relevant details',adminReply:'Explain the resolution and next steps',comments:'Tell us about your service experience',reason:'Explain the reason for this request',
  deactivationReason:'Explain why this account should be deactivated',refundReason:'Explain why a refund is needed',shortDescription:'Describe what this service includes',subject:'e.g. Help rescheduling my appointment',title:'e.g. Workshop opening hours update',
  partName:'e.g. Front brake pads',supplierName:'e.g. Lanka Auto Supplies',company:'e.g. Auto Parts Lanka',category:'Choose a suitable category',fuelType:'Choose a fuel grade',
  pumpName:'e.g. Pump 01',bayName:'e.g. Service Bay 01',station:'e.g. Colombo Main Station',assignedStation:'e.g. Colombo Main Station',goodsItem:'e.g. Oil filters',
  deliverySchedule:'e.g. Every Monday, 9 am to noon',estimatedDuration:'e.g. 1 Hour 30 Minutes',quantity:'e.g. 2',stockQuantity:'e.g. 100',minStockWarning:'e.g. 10',
  currentStockLitres:'e.g. 5000.00',maxCapacityLitres:'e.g. 10000.00',rating:'Choose a score from 1 to 5',discountPercentage:'e.g. 10',durationHours:'e.g. 1.5',
  name:'e.g. Engine Oil Change',pumpNumber:'e.g. P-01',location:'e.g. Forecourt, lane 2',assignedOperator:'e.g. Nimal Perera',icon:'e.g. fa-wrench',
  staffId:'Choose an employee',mechanicId:'Choose an available mechanic',bayId:'Choose an available service bay',jobCardId:'Choose a completed service',
  paymentMethod:'Choose how to pay',role:'Choose a job designation',popular:'Choose whether to feature this service',priority:'Choose the urgency of your request',
  leaveType:'Choose the type of leave',employmentStatus:'Choose an employment state',status:'Choose an available state',shiftTiming:'Choose a shift period',
  serviceType:'Choose a service package',timeSlot:'Choose an available start time',newSlot:'Choose an available start time',supplier:'Choose a supplier',targetId:'Choose the item to promote',
};
export function placeholderFor(rule,provided){
  const candidate=String(provided??'').trim();
  if(candidate&&!/^(optional|[•*]+)$/i.test(candidate))return candidate;
  if(rule.kind==='year')return 'Select year';
  if(rule.kind==='phone')return 'e.g. +94771234567';
  if(rule.kind==='password')return rule.key==='confirmPassword'?'Re-enter your new password':rule.newPassword?'Use at least 8 characters':'Enter the password for your account';
  if(rule.kind==='email')return examples.email;
  if(rule.kind==='name'&&rule.key==='name')return examples.fullName;
  if(examples[rule.key])return examples[rule.key];
  if(rule.kind==='search')return 'Search records...';
  if(rule.kind==='number')return rule.integer?'e.g. 10':'e.g. 1500.00';
  if(rule.kind==='name')return examples.fullName;
  return rule.multiline?'Add relevant details here':'e.g. '+(/name/i.test(rule.key)?'Full Service':'Workshop details');
}

export function normalizePhone(value){
  let clean=String(value??'').trim().replace(/[ ()-]/g,'');
  if(clean.startsWith('00'))clean='+'+clean.slice(2);
  if(/^0[1-9]\d{8}$/.test(clean))clean='+94'+clean.slice(1);
  else if(/^[1-9]\d{8}$/.test(clean))clean='+94'+clean;
  return clean;
}

export function normalizeText(value,{multiline=false,final=true}={}){
  let clean=String(value??'').replace(/\r\n?/g,'\n');
  clean=multiline?clean.replace(/[^\S\n]+/g,' ').replace(/^ +/gm,'').replace(/ +\n/g,'\n').trimStart():clean.replace(/\s+/g,' ').trimStart();
  return final?(multiline?clean.replace(/ +$/gm,''):clean).trim():clean;
}

function sanitizeEmail(value,previous){
  const clean=value.replace(/\s/g,'');
  const [local,domain='']=clean.split('@');
  if((clean.match(/@/g)||[]).length>1||local.length>64||domain.length>253||domain.split('.').some(part=>part.length>63)||clean.length>254)return String(previous);
  if(!/^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]*$/.test(local)||local.startsWith('.')||local.includes('..')||clean.includes('@')&&(!local||local.endsWith('.')))return String(previous);
  if(/[^A-Za-z0-9.-]/.test(domain))return String(previous);
  const labels=domain.split('.');
  if(domain&&labels.some((part,index)=>part===''?index!==labels.length-1:/^[^A-Za-z0-9]/.test(part)||index<labels.length-1&&part.endsWith('-')))return String(previous);
  return clean;
}

// Keep a single trailing separator while editing so the next word can be typed.
// Passwords, tokens and uploaded resources retain their exact contents.
export function sanitizeInput(value,rule,{final=false,previous=''}={}){
  let clean=String(value??'');
  if(rule.kind==='password')return utf8Length(clean)>72?String(previous):clean;
  if(rule.kind==='choice'||rule.kind==='date'||rule.kind==='schedule'||rule.kind==='year')return clean;
  clean=normalizeText(clean,{multiline:rule.multiline,final:false});
  if(rule.kind==='name')clean=clean.replace(/[^\p{L}\p{M} ]/gu,'').trimStart();
  if(rule.kind==='place')clean=clean.replace(/[^\p{L}\p{M} .’'-]/gu,'').replace(/^[^\p{L}\p{M}]+/u,'');
  if(rule.kind==='catalog')clean=clean.replace(/[^\p{L}\p{M}\p{N} &().,’'/+_-]/gu,'').replace(/^[^\p{L}\p{M}\p{N}]+/u,'');
  if(rule.kind==='address')clean=clean.replace(/[^\p{L}\p{M}\p{N}\u200c\u200d .,/#’'()&-]/gu,'');
  if(rule.kind==='username')clean=clean.replace(/[^A-Za-z0-9._-]/g,'').replace(/^[^A-Za-z0-9]+/,'');
  if(rule.kind==='identifier')clean=clean.includes('@')?sanitizeEmail(clean,previous):clean.replace(/[^A-Za-z0-9@.!#$%&'*+/=?^_`{|}~-]/g,'');
  if(rule.kind==='email')clean=sanitizeEmail(clean,previous);
  if(rule.kind==='phone'){
    clean=normalizePhone(clean).replace(/[^+\d]/g,'').replace(/(?!^)\+/g,'');
    if(clean.startsWith('+940'))clean='+94'+clean.slice(4);
    if(final&&clean==='+94')clean='';
  }
  if(rule.kind==='plate')clean=clean.replace(/[^A-Za-z0-9 -]/g,'').toUpperCase();
  if(rule.kind==='reference'||rule.kind==='invoice')clean=clean.replace(/[^A-Za-z0-9-]/g,'');
  if(rule.kind==='identifierCode')clean=clean.replace(/[^A-Za-z0-9 /-]/g,'').replace(/^[^A-Za-z0-9]+/,'');
  if(rule.kind==='duration')clean=clean.replace(/[^A-Za-z0-9. ]/g,'');
  if(rule.kind==='number'){
    // Reject signs/exponents and integer fractions instead of changing their value.
    if(/[eE+-]/.test(clean)||rule.integer&&clean.includes('.'))return String(previous);
    clean=clean.replace(/[^\d.]/g,'');
    if((clean.match(/\./g)||[]).length>1)return String(previous);
    const [whole,fraction]=clean.split('.');
    if(fraction!=null)clean=whole+'.'+fraction.slice(0,rule.decimals??2);
    if(clean&&rule.max!=null&&Number(clean)>Number(rule.max))return String(previous);
    if(clean.length>16)return String(previous);
  }
  clean=normalizeText(clean,{multiline:rule.multiline,final});
  return rule.maxLength?clean.slice(0,rule.maxLength):clean;
}

export function normalizePayload(value,key=''){
  if(Array.isArray(value))return value.map(item=>normalizePayload(item));
  if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([field,item])=>[field,normalizePayload(item,field)]));
  if(typeof value!=='string'||/password|token|hash|signature|image|url|key$/i.test(key))return value;
  const clean=normalizeText(value,{multiline:true});
  return /phone|contactNumber|ownerContact/i.test(key)?normalizePhone(clean):key==='deliverySchedule'?normalizeDeliverySchedule(clean):clean;
}
export function validateValue(value,rule,form={}){
  const {kind,label='This field'}=rule,raw=String(value??''),clean=kind==='password'?raw:raw.trim();
  if(!clean.trim())return rule.required?`${label} is required`:'';
  if(rule.options){const options=rule.options.map(item=>String(typeof item==='object'?item.value:item));if(!options.includes(raw))return 'Choose an available option';}
  if(kind==='password'){
    if(utf8Length(raw)>72)return 'Use at most 72 UTF-8 bytes for this password';
    if(/[\u0000-\u001f\u007f]/.test(raw))return 'Remove control characters from the password';
    if(rule.newPassword&&[...raw].length<8)return 'Use at least 8 characters; a longer passphrase works well';
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
  if(kind==='name'&&!humanName.test(clean))return 'Use letters with a single space between names';
  if(kind==='place'&&!/^[\p{L}\p{M}][\p{L}\p{M} .’'-]*$/u.test(clean))return 'Use letters and spaces for this location';
  if(kind==='catalog'&&!/^[\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N} &().,’'/+_-]*$/u.test(clean))return 'Use letters, numbers and relevant punctuation';
  if(kind==='address'&&(!/^[\p{L}\p{M}\p{N}\u200c\u200d .,/#’'()&-]+$/u.test(clean)||/(?<![\p{L}\p{M}])[\u200c\u200d]|[\u200c\u200d](?![\p{L}\p{M}])/u.test(clean)))return 'Use letters, numbers and address punctuation';
  if(kind==='address'&&(!/\p{L}/u.test(clean)||/([.,/#'()&-])\1{2,}/.test(clean)))return 'Enter a meaningful address with a street or locality';
  if(kind==='username'&&!username.test(clean))return 'Use 3–40 letters/numbers, dots, underscores or hyphens; start with a letter/number';
  if(kind==='email'){
    const [local,domain='']=clean.split('@');
    if(local.length>64)return 'Use at most 64 characters before @';
    if(domain.length>253||domain.split('.').some(part=>part.length>63))return 'Use at most 63 characters in each domain label';
    if(!email.test(clean)||local.startsWith('.')||local.endsWith('.')||local.includes('..'))return 'Enter a valid email address, such as name@example.com';
  }
  if(kind==='phone'&&!validPhone(clean,rule.phoneCountry))return 'Enter a valid phone number for the selected country';
  if(kind==='schedule'){const issue=deliveryScheduleError(clean);if(issue)return issue;}
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
  if(kind==='year'){
    if(!clean)return rule.required?`${label} is required`:'';
    if(!/^\d{4}$/.test(clean))return 'Select a valid 4-digit year';
    const n=Number(clean);
    if(rule.min!=null&&n<Number(rule.min))return `Select ${rule.min} or later`;
    if(rule.max!=null&&n>Number(rule.max))return `Select ${rule.max} or earlier`;
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
  for(const field of fields){const options=field.allowCustom?undefined:typeof field.options==='function'?field.options(values):field.options;const issue=validateValue(values[field.key],ruleFor(field.key,field.label,{...field,options,required:!field.optional,newPassword:field.newPassword??(field.type==='password'&&field.key!=='currentPassword')}),values);if(issue)issues[field.key]=issue;}
  return issues;
}
