package com.fuelstation.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.NumberParseException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

/** Format/range validation before controllers use incoming JSON or multipart records.
 * Ownership, availability, pricing and transactional rules remain in their services. */
public final class InputValidation {
    private InputValidation() {}
    public static final class InvalidFields extends IllegalArgumentException {
        private final Map<String,String> fields;
        public InvalidFields(Map<String,String> fields) { super(fields.values().iterator().next()); this.fields=Map.copyOf(fields); }
        public Map<String,String> fields() { return fields; }
    }
    private static final Pattern NAME=Pattern.compile("^[\\p{L}\\p{M}]+(?: [\\p{L}\\p{M}]+)*$");
    private static final Set<String> CATALOG=Set.of("partName","supplierName","company","category","fuelType","pumpName","bayName","make","model","station","assignedStation","role");
    private static final Pattern EMAIL=Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");
    private static final PhoneNumberUtil PHONES=PhoneNumberUtil.getInstance();
    private static final Pattern PLATE=Pattern.compile("^(?:(?:WP|CP|SP|NP|EP|NW|NC|UP|SG)\\s+)?(?:[A-Z]{1,3}|\\d{1,3})[ -]?\\d{4}$",Pattern.CASE_INSENSITIVE);
    private static final Set<String> NAMES=Set.of("fullName","firstName","lastName","ownerName","customerName","staffName","assignedOperator");
    private static final Set<String> USERNAMES=Set.of("username","systemUsername","ownerUsername","customerUsername");
    private static final Set<String> LONG_TEXT=Set.of("mechanicNotes","problemDescription","adminReply","shortDescription","partsUsed");
    private static final Set<String> MONEY=Set.of("amount","paymentAmount","cashTendered","currentPrice","newPrice","buyingPrice","sellingPrice","estimatedCost","collectedAmount","litersSold","quantityLitres","currentStockLitres","maxCapacityLitres","minStockWarning","discountPercentage","currentMeterReading","openingReading","closingReading");
    private static final Set<String> INTEGERS=Set.of("quantity","stockQuantity","mileage","manufactureYear","rating");
    private static final Set<String> BOOLEAN=Set.of("emailNotifications","smsNotifications","popular","active","qrConfirmed","removeImage","confirmed");
    private static final Set<String> IDS=Set.of("id","staffId","pumpId","partId","jobCardId","mechanicId","bayId","targetId","invoiceId","serviceId");
    private static final Set<String> TEXT_FIELDS=Set.of("name","licensePlate","vehicleNumber","identifier","referenceNumber","invoiceNumber","engineNumber","chassisNumber","notes","mechanicNotes","problemDescription","message","adminReply","comments","reason","refundReason","deactivationReason","shortDescription","subject","title","category","company","partName","supplierName","fuelType","pumpName","bayName","make","model","address","city","country","station","assignedStation","estimatedDuration","role","status","employmentStatus","shiftTiming","leaveType","paymentMethod");
    public static String normalizeText(String raw,boolean multiline) {
        if(raw==null)return null;
        return multiline?raw.replaceAll("\\r\\n?","\n").replaceAll("(?U)[^\\S\\n]+"," ").replaceAll("(?m)^ +| +$","").strip():raw.replaceAll("(?U)\\s+"," ").strip();
    }
    public static String normalizePhone(String raw) {
        if(raw==null)return null;
        String value=raw.strip().replaceAll("[ ()-]","");
        if(value.startsWith("00"))value="+"+value.substring(2);
        if(value.matches("0[1-9]\\d{8}"))value="+94"+value.substring(1);
        else if(value.matches("[1-9]\\d{8}"))value="+94"+value;
        return value;
    }
    /** Normalize before Jackson binds request values, so persisted fields match validation. */
    public static void normalize(JsonNode node) {
        if(node.isArray()){node.forEach(InputValidation::normalize);return;}
        if(!(node instanceof ObjectNode object))return;
        node.fields().forEachRemaining(entry -> {
            String key=entry.getKey();JsonNode value=entry.getValue();
            if(value.isContainerNode()){normalize(value);return;}
            if(!value.isTextual()||key.matches("(?i).*(?:password|token|hash|signature|image|url|key$).*"))return;
            String clean=normalizeText(value.asText(),true);
            if(key.toLowerCase(Locale.ROOT).contains("phone")||key.equals("contactNumber")||key.equals("ownerContact"))clean=normalizePhone(clean);
            if(key.equals("deliverySchedule"))clean=DeliverySchedule.normalize(clean);
            object.put(key,clean);
        });
    }
    public static String text(String raw,String label,int max,boolean required,boolean multiline) {
        if(raw==null||raw.isBlank()) { if(required)throw new IllegalArgumentException(label+" is required");return raw==null?null:""; }
        String value=normalizeText(raw,multiline);
        if(raw.length()>max)throw new IllegalArgumentException(label+" must have at most "+max+" characters");
        if(value.codePoints().anyMatch(c -> (c<32&&!(multiline&&(c==9||c==10||c==13)))||c==127))throw new IllegalArgumentException("Remove control characters from "+label.toLowerCase(Locale.ROOT));
        return value;
    }
    public static String name(String raw,String label,boolean required) {
        String value=text(raw,label,100,required,false);if(value==null||value.isEmpty())return value;
        if(value.codePointCount(0,value.length())<2||!NAME.matcher(value).matches())throw new IllegalArgumentException(label+" must use letters with a single space between names");return value;
    }
    public static String username(String raw,boolean required) {
        String value=text(raw,"Username",40,required,false);if(value==null||value.isEmpty())return value;
        if(!value.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,39}"))throw new IllegalArgumentException("Username must have 3–40 letters/numbers, dots, underscores or hyphens and start with a letter/number");return value;
    }
    public static String email(String raw,boolean required) {
        String value=text(raw,"Email",254,required,false);if(value==null||value.isEmpty())return value;
        String local=value.split("@",-1)[0];
        if(local.length()>64)throw new IllegalArgumentException("Use at most 64 characters before @");
        if(!EMAIL.matcher(value).matches()||local.startsWith(".")||local.endsWith(".")||local.contains(".."))throw new IllegalArgumentException("Enter a valid email address with domain labels of at most 63 characters");return value;
    }
    public static String phone(String raw,String label,boolean required) {
        String value=normalizePhone(text(raw,label,25,required,false));if(value==null||value.isEmpty())return value;
        boolean valid=false;
        if(value.matches("\\+[1-9]\\d{6,14}"))try {
            var number=PHONES.parse(value,null);
            valid=PHONES.isValidNumber(number)&&PHONES.format(number,PhoneNumberUtil.PhoneNumberFormat.E164).equals(value);
        }catch(NumberParseException ignored){}
        if(!valid)throw new IllegalArgumentException("Enter a valid phone number with its country code");return value;
    }
    public static String address(String raw,String label,int max,boolean required) {
        String value=text(raw,label,max,required,false);if(value==null||value.isEmpty())return value;
        if(value.codePointCount(0,value.length())<5||!value.matches("[\\p{L}\\p{M}\\p{N}\\u200c\\u200d .,/#’'()&-]+")||!value.matches(".*\\p{L}.*")||Pattern.compile("([.,/#'()&-])\\1{2,}|(?<![\\p{L}\\p{M}])[\\u200c\\u200d]|[\\u200c\\u200d](?![\\p{L}\\p{M}])").matcher(value).find())throw new IllegalArgumentException("Enter a meaningful address of 5–"+max+" characters with a street or locality");
        return value;
    }
    public static String plate(String raw,boolean required) {
        String value=text(raw,"Vehicle plate",20,required,false);if(value==null||value.isEmpty())return value;
        if(!PLATE.matcher(value).matches())throw new IllegalArgumentException("Use a valid plate, e.g. ABC-1234 or WP CAA-1234");return value.toUpperCase(Locale.ROOT);
    }
    public static void password(String value,boolean newPassword) {
        if(value==null||value.isBlank())throw new IllegalArgumentException("Password is required");
        if(value.getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Use at most 72 UTF-8 bytes for the password");
        if(value.codePoints().anyMatch(c -> c<32||c==127))throw new IllegalArgumentException("Remove control characters from the password");
        if(newPassword&&value.codePointCount(0,value.length())<8)throw new IllegalArgumentException("Use at least 8 characters for a new password");
    }
    public static BigDecimal decimal(String raw,String label,double min,double max,boolean integer) {
        if(raw==null||!raw.matches("-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"))throw new IllegalArgumentException("Enter a valid "+label.toLowerCase(Locale.ROOT));
        BigDecimal n;
        try { n=new BigDecimal(raw); } catch(NumberFormatException e) {throw new IllegalArgumentException("Enter a valid number");}
        if(integer&&n.stripTrailingZeros().scale()>0)throw new IllegalArgumentException(label+" must be a whole number");
        if(!integer&&n.stripTrailingZeros().scale()>2)throw new IllegalArgumentException(label+" allows at most two decimal places");
        if(n.compareTo(BigDecimal.valueOf(min))<0||n.compareTo(BigDecimal.valueOf(max))>0)throw new IllegalArgumentException(label+" must be between "+min+" and "+max);return n;
    }
    public static void validate(JsonNode root,String path) {
        validate(root,path,LocalDate.now(ZoneId.of("Asia/Colombo")));
    }
    public static void validate(JsonNode root,String path,LocalDate today) {
        Map<String,String> errors=new LinkedHashMap<>();walk(root,path,"",errors,today);
        if(root.isObject()){requiredFields(root,path,errors);relationships(root,path,errors);}
        if(!errors.isEmpty())throw new InvalidFields(errors);
    }
    private static void walk(JsonNode node,String path,String prefix,Map<String,String> errors,LocalDate today) {
        if(node.isArray()) {
            if(node.size()>100)errors.put(prefix,"Use at most 100 items per request");
            for(int i=0;i<Math.min(100,node.size());i++)walk(node.get(i),path,prefix+"["+i+"].",errors,today);return;
        }
        if(!node.isObject())return;
        node.fields().forEachRemaining(entry -> {
            String key=entry.getKey(),field=prefix+key;JsonNode value=entry.getValue();
            if(value.isObject()||value.isArray()) {walk(value,path,field+".",errors,today);return;}
            if(value.isNull())return;
            try {check(key,value,path,today);} catch(IllegalArgumentException e) {errors.put(field,e.getMessage());}
        });
    }
    private static void check(String key,JsonNode node,String path,LocalDate today) {
        String value=node.asText(),label=key.replaceAll("([a-z])([A-Z])","$1 $2");
        if(BOOLEAN.contains(key)) {if(!node.isBoolean())throw new IllegalArgumentException("Choose a valid on/off value");return;}
        if(!node.isTextual()&&(TEXT_FIELDS.contains(key)||NAMES.contains(key)||USERNAMES.contains(key)||key.toLowerCase(Locale.ROOT).contains("email")||key.toLowerCase(Locale.ROOT).contains("phone")||key.toLowerCase(Locale.ROOT).contains("password")||key.equals("contactNumber")||key.equals("ownerContact")))throw new IllegalArgumentException("Enter a valid text value for "+label);
        if(key.equals("version")){decimal(value,label,0,9007199254740991d,true);return;}
        if(key.equals("password")||key.equals("newPassword")||key.equals("currentPassword")||key.equals("confirmPassword")) {
            if(value.isEmpty()&&path.contains("/users/update/"))return;
            password(value,!key.equals("currentPassword")&&!path.endsWith("/login"));return;
        }
        if(USERNAMES.contains(key)) {username(value,false);return;}
        if(NAMES.contains(key)||key.equals("name")&&path.contains("/hr/staff")) {name(value,label,false);if(key.equals("name")&&value.length()>60)throw new IllegalArgumentException("Staff name allows at most 60 characters");return;}
        if(key.toLowerCase(Locale.ROOT).contains("email")) {email(value,false);return;}
        if(key.toLowerCase(Locale.ROOT).contains("phone")||key.equals("contactNumber")||key.equals("ownerContact")) {phone(value,label,false);return;}
        if(key.equals("licensePlate")||key.equals("vehicleNumber")) {plate(value,false);return;}
        if(key.equals("identifier")) {if(value.contains("@"))email(value,true);else username(value,true);return;}
        if(key.equals("address")) {address(value,"Address",255,false);return;}
        if(key.equals("deliverySchedule")) {DeliverySchedule.validate(value);return;}
        if(key.equals("referenceNumber")) {if(!value.isBlank()&&!value.matches("[A-Za-z0-9][A-Za-z0-9-]{2,63}"))throw new IllegalArgumentException("Enter a valid service reference number");return;}
        if(key.equals("invoiceNumber")) {if(!value.isBlank()&&!value.matches("(?i)(?:INV|SP|SRV|FUEL)-[A-Za-z0-9-]{1,60}"))throw new IllegalArgumentException("Enter a valid invoice number");return;}
        if(IDS.contains(key)) {decimal(value,label,1,9007199254740991d,true);return;}
        if(INTEGERS.contains(key)) {
            double min=key.equals("quantity")||key.equals("rating")?1:key.equals("manufactureYear")?1900:0;
            double max=key.equals("quantity")?9999:key.equals("rating")?5:key.equals("manufactureYear")?today.getYear()+1:Integer.MAX_VALUE;
            decimal(value,label,min,max,true);return;
        }
        if(MONEY.contains(key)) {decimal(value,label,key.equals("maxCapacityLitres")?.01:0,key.equals("discountPercentage")?100:999999999.99,key.equals("minStockWarning")&&path.contains("inventory/parts"));return;}
        if(key.endsWith("Date")||key.equals("newDate")) {
            if(value.isBlank())return;
            LocalDate date;
            try {date=LocalDate.parse(value);}catch(RuntimeException e){throw new IllegalArgumentException("Enter a real date");}
            if(date.getYear()<1900||date.getYear()>2100)throw new IllegalArgumentException("Choose a date between 1900 and 2100");
            if(Set.of("serviceDate","newDate","shiftDate","startDate").contains(key)&&date.isBefore(today))throw new IllegalArgumentException("Choose today or a future date");
            if(key.equals("registeredDate")&&date.isAfter(today))throw new IllegalArgumentException("Registration date cannot be in the future");return;
        }
        if(!node.isTextual())return;
        int max=LONG_TEXT.contains(key)?4000:key.equals("notes")&&(path.contains("bookings")||path.contains("booking"))?4000:key.equals("message")&&path.contains("support")?4000:key.equals("email")?254:key.equals("city")||key.equals("country")?80:CATALOG.contains(key)||key.equals("name")?100:key.equals("title")?150:key.equals("subject")?200:255;
        if(key.equals("imageUrl"))max=1000;
        text(value,label,max,false,Set.of("notes","message","adminReply","shortDescription","mechanicNotes","problemDescription","comments","reason").contains(key));
        if((CATALOG.contains(key)||key.equals("name")||key.equals("country"))&&!value.isBlank()&&!value.matches("[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N} &().,’'/+_-]*"))throw new IllegalArgumentException("Use letters, numbers and relevant punctuation for "+label);
        if(key.equals("city")&&!value.isBlank()&&!value.matches("[\\p{L}\\p{M}][\\p{L}\\p{M} .’'-]*"))throw new IllegalArgumentException("Use letters and spaces for this location");
        if(Set.of("address","location").contains(key)&&!value.isBlank()&&!value.matches("[\\p{L}\\p{M}\\p{N} .,/#’'()&-]+"))throw new IllegalArgumentException("Use letters, numbers and address punctuation");
        if(key.equals("pumpNumber")&&!value.isBlank()&&!value.matches("[A-Za-z0-9][A-Za-z0-9 /-]{0,19}"))throw new IllegalArgumentException("Use letters, numbers or hyphens for the pump number");
        if(Set.of("city","country","partName","fuelType","pumpName","bayName","make","model","company","supplierName","name","category","assignedStation","station").contains(key)&&!value.isBlank()&&value.trim().length()<2)throw new IllegalArgumentException(label+" must have at least two characters");
        if(Set.of("engineNumber","chassisNumber").contains(key)&&!value.isBlank()&&!value.matches("[A-Za-z0-9][A-Za-z0-9 /-]{0,49}"))throw new IllegalArgumentException("Use a valid engine/chassis identifier");
        if(Set.of("reason","refundReason","deactivationReason").contains(key)&&!value.isBlank()&&value.trim().length()<3)throw new IllegalArgumentException(label+" must have at least three characters");
        choices(key,value,path);
    }
    private static void choices(String key,String value,String path) {
        Set<String> allowed=null;
        if(key.equals("leaveType"))allowed=Set.of("Annual","Sick","Casual","Emergency","Unpaid");
        if(key.equals("employmentStatus"))allowed=Set.of("Active","Inactive","On Leave");
        if(key.equals("pumpStatus"))allowed=Set.of("Active","Inactive");
        if(key.equals("shiftTiming"))allowed=Set.of("Morning (08:00 - 16:00)","Evening (16:00 - 00:00)","Night (00:00 - 08:00)");
        if(key.equals("role")&&path.contains("/users"))allowed=Set.of("Admin","Manager","Cashier","Mechanic","Customer");
        if(key.equals("status")&&path.contains("/users"))allowed=Set.of("Active","Deactivated");
        if(key.equals("status")&&path.contains("/pumps"))allowed=Set.of("Active","Maintenance","Disabled");
        if(key.equals("status")&&path.contains("/shifts"))allowed=Set.of("Scheduled","Active","Completed","Swapped","Cancelled");
        if(key.equals("status")&&path.contains("inventory/suppliers"))allowed=Set.of("Active","Inactive");
        if(allowed!=null&&!allowed.contains(value))throw new IllegalArgumentException("Choose an available "+key);
        if(key.equals("paymentMethod"))Rules.paymentMethod(value);
    }
    private static void relationships(JsonNode node,String path,Map<String,String> errors) {
        if(has(node,"startDate")&&has(node,"endDate")&&node.get("endDate").asText().compareTo(node.get("startDate").asText())<0)errors.put("endDate","End date must be on or after the start date");
        if(has(node,"maxCapacityLitres"))for(String key:List.of("currentStockLitres","minStockWarning"))if(has(node,key)&&node.get(key).asDouble()>node.get("maxCapacityLitres").asDouble())errors.put(key,"Stock and warning levels cannot exceed storage capacity");
        if(node.has("confirmPassword")) {String key=node.has("newPassword")?"newPassword":"password";if(!node.path(key).asText().equals(node.path("confirmPassword").asText()))errors.put("confirmPassword","Passwords do not match");}
        if(has(node,"currentPassword")&&has(node,"newPassword")&&node.get("currentPassword").asText().equals(node.get("newPassword").asText()))errors.put("newPassword","Choose a password different from your current password");
    }
    private static void requiredFields(JsonNode node,String path,Map<String,String> errors) {
        List<String> fields=List.of();
        if(path.equals("/api/auth/signup"))fields=List.of("fullName","username","email","password","confirmPassword");
        else if(path.equals("/api/auth/reset-password"))fields=List.of("newPassword","confirmPassword","token");
        else if(path.equals("/api/auth/forgot-password"))fields=List.of("identifier");
        else if(path.equals("/api/v1/users/add"))fields=List.of("fullName","username","email","password","role");
        else if(path.equals("/api/v1/account"))fields=List.of("fullName","email");
        else if(path.equals("/api/v1/account/password"))fields=List.of("currentPassword","newPassword","confirmPassword");
        else if(path.matches("/api/inventory/(fuel|parts|suppliers)/(add|update/\\d+)"))fields=path.contains("/fuel/")?List.of("fuelType","currentStockLitres","maxCapacityLitres","minStockWarning"):path.contains("/parts/")?List.of("partName","category","stockQuantity","minStockWarning","buyingPrice","sellingPrice"):List.of("supplierName","company","contactNumber","status");
        else if(path.matches("/api/services/(add|update/\\d+)"))fields=List.of("name","category","estimatedCost","estimatedDuration");
        else if(path.matches("/api/v1/hr/staff/(add|update/\\d+)"))fields=List.of("name","role","phone","email","assignedStation");
        else if(path.equals("/api/v1/hr/leave/add"))fields=List.of("staffId","leaveType","startDate","endDate","reason");
        else if(path.equals("/api/v1/hr/self/leave"))fields=List.of("leaveType","startDate","endDate","reason");
        else if(path.equals("/api/v1/notifications/announcements"))fields=List.of("title","message");
        else if(path.equals("/api/v1/customer/support"))fields=List.of("subject","category","priority","message");
        else if(path.matches("/api/v1/support/admin/reply/\\d+"))fields=List.of("adminReply","status");
        for(String key:fields)if(!has(node,key))errors.putIfAbsent(key,key.replaceAll("([a-z])([A-Z])","$1 $2")+" is required");
    }
    private static boolean has(JsonNode node,String key) {return node.hasNonNull(key)&&!node.get(key).asText().isBlank();}
}
