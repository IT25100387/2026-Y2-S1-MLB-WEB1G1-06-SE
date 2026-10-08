export const vehicleCatalog={
  Toyota:['Aqua','Axio','Corolla','Hiace','Hilux','Land Cruiser','Passo','Premio','Prius','RAV4','Vios','Vitz','Yaris'],
  Honda:['Accord','BR-V','Civic','CR-V','Dio','Fit','Grace','HR-V','Insight','Vezel'],
  Nissan:['Almera','Caravan','Dayz','Juke','Leaf','March','Note','Patrol','Sunny','X-Trail'],
  Suzuki:['Alto','Baleno','Celerio','Every','Jimny','Swift','Wagon R'],
  Mitsubishi:['L200','Lancer','Mirage','Montero','Outlander','Pajero'],
  Mazda:['Axela','CX-3','CX-5','Demio','Mazda 3','Mazda 6'],
  Daihatsu:['Hijet','Mira','Move','Terios'],
  Hyundai:['Accent','Creta','Grand i10','i20','Santa Fe','Tucson'],
  Kia:['Picanto','Rio','Seltos','Sorento','Sportage'],
  Ford:['EcoSport','Fiesta','Focus','Ranger','Transit'],
  BMW:['1 Series','3 Series','5 Series','X1','X3','X5'],
  'Mercedes-Benz':['A-Class','C-Class','E-Class','GLA','GLC','Vito'],
  Audi:['A3','A4','A6','Q3','Q5','Q7'],
  Volkswagen:['Golf','Polo','Tiguan','Transporter'],
  Peugeot:['206','208','3008','406'],
  Renault:['Duster','Kwid','Megane'],
  Tata:['Ace','Indica','Nano','Nexon','Safari'],
  Mahindra:['Bolero','KUV100','Scorpio','Thar','XUV500'],
  Isuzu:['D-Max','Elf','MU-X'],
  BYD:['Atto 3','Dolphin','Seal'],
  Tesla:['Model 3','Model S','Model X','Model Y'],
  Bajaj:['CT 100','Discover','Platina','Pulsar','RE'],
  Hero:['Glamour','HF Deluxe','Hunk','Splendor'],
  TVS:['Apache','Jupiter','Ntorq','Scooty Pep'],
  Yamaha:['FZ','NMAX','Ray ZR','YZF-R15'],
};
export const withCurrent=(options,value)=>value&&!options.some(option=>String(typeof option==='object'?option.value:option)===String(value))?[...options,value]:options;
export const vehicleMakes=value=>withCurrent(Object.keys(vehicleCatalog).sort(),value);
export const vehicleModels=(make,value)=>withCurrent(vehicleCatalog[make]||[],value);
export const fuelGrades=['Lanka Petrol 92','Lanka Petrol 95','Lanka Auto Diesel','Lanka Super Diesel','Petrol 92','Petrol 95','Auto Diesel','Super Diesel'];
export const partCategories=['Braking System','Engine Parts','Filters','Lubricants','Electrical','Suspension','Transmission','Tyres & Wheels','Accessories'];
export const serviceCategories=['Maintenance','Full Service','Repair','Inspection','Engine','Brakes','Electrical','Tyres & Wheels','Cleaning'];
export const staffRoles=['Manager','Cashier','Mechanic','Supervisor','Pump Operator','Service Advisor','Administrator'];
const countryNames=new Intl.DisplayNames(['en'],{type:'region'});
export const billingCountries=('AF AL DZ AS AD AO AI AQ AG AR AM AW AU AT AZ BS BH BD BB BY BE BZ BJ BM BT BO BA BW BR IO BN BG BF BI KH CM CA CV KY CF TD CL CN CX CC CO KM CG CD CK CR CI HR CU CW CY CZ DK DJ DM DO EC EG SV GQ ER EE SZ ET FK FO FJ FI FR GF PF TF GA GM GE DE GH GI GR GL GD GP GU GT GG GN GW GY HT HN HK HU IS IN ID IR IQ IE IM IL IT JM JP JE JO KZ KE KI KP KR KW KG LA LV LB LS LR LY LI LT LU MO MG MW MY MV ML MT MH MQ MR MU YT MX FM MD MC MN ME MS MA MZ MM NA NR NP NL NC NZ NI NE NG NU NF MK MP NO OM PK PW PS PA PG PY PE PH PN PL PT PR QA RE RO RU RW BL SH KN LC MF PM VC WS SM ST SA SN RS SC SL SG SX SK SI SB SO ZA GS SS ES LK SD SR SJ SE CH SY TW TJ TZ TH TL TG TK TO TT TN TR TM TC TV UG UA AE GB US UM UY UZ VU VA VE VN VG VI WF EH YE ZM ZW').split(' ').map(code=>countryNames.of(code)).sort();
