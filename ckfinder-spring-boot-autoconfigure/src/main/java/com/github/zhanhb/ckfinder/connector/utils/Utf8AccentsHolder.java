package com.github.zhanhb.ckfinder.connector.utils;

/**
 *
 * @author zhanhb
 */
class Utf8AccentsHolder {

  public static String convert(String raw) {
    int len = raw.length();
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char ch = raw.charAt(i);
      switch (ch) {
        case 181:
          sb.append('u');
          break;
        case 192:
          sb.append('A');
          break;
        case 193:
          sb.append('A');
          break;
        case 194:
          sb.append('A');
          break;
        case 195:
          sb.append('A');
          break;
        case 196:
          sb.append("Ae");
          break;
        case 197:
          sb.append('A');
          break;
        case 198:
          sb.append("Ae");
          break;
        case 199:
          sb.append('C');
          break;
        case 200:
          sb.append('E');
          break;
        case 201:
          sb.append('E');
          break;
        case 202:
          sb.append('E');
          break;
        case 203:
          sb.append('E');
          break;
        case 204:
          sb.append('I');
          break;
        case 205:
          sb.append('I');
          break;
        case 206:
          sb.append('I');
          break;
        case 207:
          sb.append('I');
          break;
        case 208:
          sb.append("Dh");
          break;
        case 209:
          sb.append('N');
          break;
        case 210:
          sb.append('O');
          break;
        case 211:
          sb.append('O');
          break;
        case 212:
          sb.append('O');
          break;
        case 213:
          sb.append('O');
          break;
        case 214:
          sb.append("Oe");
          break;
        case 216:
          sb.append('O');
          break;
        case 217:
          sb.append('U');
          break;
        case 218:
          sb.append('U');
          break;
        case 219:
          sb.append('U');
          break;
        case 220:
          sb.append("Ue");
          break;
        case 221:
          sb.append('Y');
          break;
        case 222:
          sb.append("Th");
          break;
        case 223:
          sb.append("ss");
          break;
        case 224:
          sb.append('a');
          break;
        case 225:
          sb.append('a');
          break;
        case 226:
          sb.append('a');
          break;
        case 227:
          sb.append('a');
          break;
        case 228:
          sb.append("ae");
          break;
        case 229:
          sb.append('a');
          break;
        case 230:
          sb.append("ae");
          break;
        case 231:
          sb.append('c');
          break;
        case 232:
          sb.append('e');
          break;
        case 233:
          sb.append('e');
          break;
        case 234:
          sb.append('e');
          break;
        case 235:
          sb.append('e');
          break;
        case 236:
          sb.append('i');
          break;
        case 237:
          sb.append('i');
          break;
        case 238:
          sb.append('i');
          break;
        case 239:
          sb.append('i');
          break;
        case 240:
          sb.append("dh");
          break;
        case 241:
          sb.append('n');
          break;
        case 242:
          sb.append('o');
          break;
        case 243:
          sb.append('o');
          break;
        case 244:
          sb.append('o');
          break;
        case 245:
          sb.append('o');
          break;
        case 246:
          sb.append("oe");
          break;
        case 248:
          sb.append('o');
          break;
        case 249:
          sb.append('u');
          break;
        case 250:
          sb.append('u');
          break;
        case 251:
          sb.append('u');
          break;
        case 252:
          sb.append("ue");
          break;
        case 253:
          sb.append('y');
          break;
        case 254:
          sb.append("th");
          break;
        case 255:
          sb.append('y');
          break;
        case 256:
          sb.append('A');
          break;
        case 257:
          sb.append('a');
          break;
        case 258:
          sb.append('A');
          break;
        case 259:
          sb.append('a');
          break;
        case 260:
          sb.append('A');
          break;
        case 261:
          sb.append('a');
          break;
        case 262:
          sb.append('C');
          break;
        case 263:
          sb.append('c');
          break;
        case 264:
          sb.append('C');
          break;
        case 265:
          sb.append('c');
          break;
        case 266:
          sb.append('C');
          break;
        case 267:
          sb.append('c');
          break;
        case 268:
          sb.append('C');
          break;
        case 269:
          sb.append('c');
          break;
        case 270:
          sb.append('D');
          break;
        case 271:
          sb.append('d');
          break;
        case 272:
          sb.append('D');
          break;
        case 273:
          sb.append('d');
          break;
        case 274:
          sb.append('E');
          break;
        case 275:
          sb.append('e');
          break;
        case 276:
          sb.append('E');
          break;
        case 277:
          sb.append('e');
          break;
        case 278:
          sb.append('E');
          break;
        case 279:
          sb.append('e');
          break;
        case 280:
          sb.append('E');
          break;
        case 281:
          sb.append('e');
          break;
        case 282:
          sb.append('E');
          break;
        case 283:
          sb.append('e');
          break;
        case 284:
          sb.append('G');
          break;
        case 285:
          sb.append('g');
          break;
        case 286:
          sb.append('G');
          break;
        case 287:
          sb.append('g');
          break;
        case 288:
          sb.append('G');
          break;
        case 289:
          sb.append('g');
          break;
        case 290:
          sb.append('G');
          break;
        case 291:
          sb.append('g');
          break;
        case 292:
          sb.append('H');
          break;
        case 293:
          sb.append('h');
          break;
        case 294:
          sb.append('H');
          break;
        case 295:
          sb.append('h');
          break;
        case 296:
          sb.append('I');
          break;
        case 297:
          sb.append('i');
          break;
        case 298:
          sb.append('I');
          break;
        case 299:
          sb.append('i');
          break;
        case 302:
          sb.append('I');
          break;
        case 303:
          sb.append('i');
          break;
        case 308:
          sb.append('J');
          break;
        case 309:
          sb.append('j');
          break;
        case 310:
          sb.append('K');
          break;
        case 311:
          sb.append('k');
          break;
        case 313:
          sb.append('L');
          break;
        case 314:
          sb.append('l');
          break;
        case 315:
          sb.append('L');
          break;
        case 316:
          sb.append('l');
          break;
        case 317:
          sb.append('L');
          break;
        case 318:
          sb.append('l');
          break;
        case 321:
          sb.append('L');
          break;
        case 322:
          sb.append('l');
          break;
        case 323:
          sb.append('N');
          break;
        case 324:
          sb.append('n');
          break;
        case 325:
          sb.append('N');
          break;
        case 326:
          sb.append('n');
          break;
        case 327:
          sb.append('N');
          break;
        case 328:
          sb.append('n');
          break;
        case 332:
          sb.append('O');
          break;
        case 333:
          sb.append('o');
          break;
        case 336:
          sb.append('O');
          break;
        case 337:
          sb.append('o');
          break;
        case 340:
          sb.append('R');
          break;
        case 341:
          sb.append('r');
          break;
        case 342:
          sb.append('R');
          break;
        case 343:
          sb.append('r');
          break;
        case 344:
          sb.append('R');
          break;
        case 345:
          sb.append('r');
          break;
        case 346:
          sb.append('S');
          break;
        case 347:
          sb.append('s');
          break;
        case 348:
          sb.append('S');
          break;
        case 349:
          sb.append('s');
          break;
        case 350:
          sb.append('S');
          break;
        case 351:
          sb.append('s');
          break;
        case 352:
          sb.append('S');
          break;
        case 353:
          sb.append('s');
          break;
        case 354:
          sb.append('T');
          break;
        case 355:
          sb.append('t');
          break;
        case 356:
          sb.append('T');
          break;
        case 357:
          sb.append('t');
          break;
        case 358:
          sb.append('T');
          break;
        case 359:
          sb.append('t');
          break;
        case 360:
          sb.append('U');
          break;
        case 361:
          sb.append('u');
          break;
        case 362:
          sb.append('U');
          break;
        case 363:
          sb.append('u');
          break;
        case 364:
          sb.append('U');
          break;
        case 365:
          sb.append('u');
          break;
        case 366:
          sb.append('U');
          break;
        case 367:
          sb.append('u');
          break;
        case 368:
          sb.append('U');
          break;
        case 369:
          sb.append('u');
          break;
        case 370:
          sb.append('U');
          break;
        case 371:
          sb.append('u');
          break;
        case 372:
          sb.append('W');
          break;
        case 373:
          sb.append('w');
          break;
        case 374:
          sb.append('Y');
          break;
        case 375:
          sb.append('y');
          break;
        case 376:
          sb.append('Y');
          break;
        case 377:
          sb.append('Z');
          break;
        case 378:
          sb.append('z');
          break;
        case 379:
          sb.append('Z');
          break;
        case 380:
          sb.append('z');
          break;
        case 381:
          sb.append('Z');
          break;
        case 382:
          sb.append('z');
          break;
        case 401:
          sb.append('F');
          break;
        case 402:
          sb.append('f');
          break;
        case 416:
          sb.append('O');
          break;
        case 417:
          sb.append('o');
          break;
        case 431:
          sb.append('U');
          break;
        case 432:
          sb.append('u');
          break;
        case 536:
          sb.append('S');
          break;
        case 537:
          sb.append('s');
          break;
        case 538:
          sb.append('T');
          break;
        case 539:
          sb.append('t');
          break;
        case 7682:
          sb.append('B');
          break;
        case 7683:
          sb.append('b');
          break;
        case 7690:
          sb.append('D');
          break;
        case 7691:
          sb.append('d');
          break;
        case 7710:
          sb.append('F');
          break;
        case 7711:
          sb.append('f');
          break;
        case 7744:
          sb.append('M');
          break;
        case 7745:
          sb.append('m');
          break;
        case 7766:
          sb.append('P');
          break;
        case 7767:
          sb.append('p');
          break;
        case 7776:
          sb.append('S');
          break;
        case 7777:
          sb.append('s');
          break;
        case 7786:
          sb.append('T');
          break;
        case 7787:
          sb.append('t');
          break;
        case 7808:
          sb.append('W');
          break;
        case 7809:
          sb.append('w');
          break;
        case 7810:
          sb.append('W');
          break;
        case 7811:
          sb.append('w');
          break;
        case 7812:
          sb.append('W');
          break;
        case 7813:
          sb.append('w');
          break;
        case 7922:
          sb.append('Y');
          break;
        case 7923:
          sb.append('y');
          break;
        default:
          sb.append(ch);
          break;
      }
    }
    return sb.toString();
  }

}
