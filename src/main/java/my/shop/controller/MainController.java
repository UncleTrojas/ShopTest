package my.shop.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import my.shop.dao.OrderDAO;
import my.shop.dao.ProductDAO;
import my.shop.email.Mail;
import my.shop.entity.Product;
import my.shop.form.CustomerForm;
import my.shop.model.CartInfo;
import my.shop.model.CartLineInfo;
import my.shop.model.CustomerInfo;
import my.shop.model.ProductInfo;
import my.shop.pagination.PaginationResult;
import my.shop.service.EmailService;
import my.shop.utils.Utils;
import my.shop.validator.CustomerFormValidator;

@Controller
@Transactional
public class MainController {
	
	@Autowired
	   private OrderDAO orderDAO;
	 
	   @Autowired
	   private ProductDAO productDAO;
	 
	   @Autowired
	   private CustomerFormValidator customerFormValidator;
	   
	   @Autowired
	   private EmailService emailService;
	 
	   @InitBinder
	   public void myInitBinder(WebDataBinder dataBinder) {
	      Object target = dataBinder.getTarget();
	      if (target == null) {
	         return;
	      }
	      System.out.println("Target=" + target);
	 
	      
	      if (target.getClass() == CartInfo.class) {
	 
	      }
	 
	      
	      else if (target.getClass() == CustomerForm.class) {
	         dataBinder.setValidator(customerFormValidator);
	      }
	 
	   }
	 
	   @RequestMapping("/403")
	   public String accessDenied() {
	      return "/403";
	   }
	 
	   @RequestMapping("/")
	   public String home() {
	      return "index";
	   }
	 
	   
	   @RequestMapping({ "/productList" })
	   public String listProductHandler(Model model, //
	         @RequestParam(value = "name", defaultValue = "") String likeName,
	         @RequestParam(value = "page", defaultValue = "1") int page) {
	      final int maxResult = 5;
	      final int maxNavigationPage = 10;
	 
	      PaginationResult<ProductInfo> result = productDAO.queryProducts(page, //
	            maxResult, maxNavigationPage, likeName);
	 
	      model.addAttribute("paginationProducts", result);
	      return "productList";
	   }
	 
	   @RequestMapping({ "/buyProduct" })
	   public String listProductHandler(HttpServletRequest request, Model model, //
	         @RequestParam(value = "code", defaultValue = "") String code) {
	 
	      Product product = null;
	      if (code != null && code.length() > 0) {
	         product = productDAO.findProduct(code);
	      }
	      if (product != null) {
	 
	         //
	         CartInfo cartInfo = Utils.getCartInSession(request);
	 
	         ProductInfo productInfo = new ProductInfo(product);
	 
	         cartInfo.addProduct(productInfo, 1);
	      }
	 
	      return "redirect:/shoppingCart";
	   }
	 
	   @RequestMapping({ "/shoppingCartRemoveProduct" })
	   public String removeProductHandler(HttpServletRequest request, Model model, //
	         @RequestParam(value = "code", defaultValue = "") String code) {
	      Product product = null;
	      if (code != null && code.length() > 0) {
	         product = productDAO.findProduct(code);
	      }
	      if (product != null) {
	 
	         CartInfo cartInfo = Utils.getCartInSession(request);
	 
	         ProductInfo productInfo = new ProductInfo(product);
	 
	         cartInfo.removeProduct(productInfo);
	 
	      }
	 
	      return "redirect:/shoppingCart";
	   }
	 
	   
	   @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.POST)
	   public String shoppingCartUpdateQty(HttpServletRequest request, //
	         Model model, //
	         @ModelAttribute("cartForm") CartInfo cartForm) {
	 
	      CartInfo cartInfo = Utils.getCartInSession(request);
	      cartInfo.updateQuantity(cartForm);
	 
	      return "redirect:/shoppingCart";
	   }
	 
	   
	   @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.GET)
	   public String shoppingCartHandler(HttpServletRequest request, Model model) {
	      CartInfo myCart = Utils.getCartInSession(request);
	 
	      model.addAttribute("cartForm", myCart);
	      return "shoppingCart";
	   }
	 
	   
	   @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.GET)
	   public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {
	 
	      CartInfo cartInfo = Utils.getCartInSession(request);
	 
	      if (cartInfo.isEmpty()) {
	 
	         return "redirect:/shoppingCart";
	      }
	      CustomerInfo customerInfo = cartInfo.getCustomerInfo();
	 
	      CustomerForm customerForm = new CustomerForm(customerInfo);
	 
	      model.addAttribute("customerForm", customerForm);
	 
	      return "shoppingCartCustomer";
	   }
	 
	   
	   @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.POST)
	   public String shoppingCartCustomerSave(HttpServletRequest request, //
	         Model model, //
	         @ModelAttribute("customerForm") @Validated CustomerForm customerForm, //
	         BindingResult result, //
	         final RedirectAttributes redirectAttributes) {
	 
	      if (result.hasErrors()) {
	         customerForm.setValid(false);
	         
	         return "shoppingCartCustomer";
	      }
	 
	      customerForm.setValid(true);
	      CartInfo cartInfo = Utils.getCartInSession(request);
	      CustomerInfo customerInfo = new CustomerInfo(customerForm);
	      cartInfo.setCustomerInfo(customerInfo);
	 
	      return "redirect:/shoppingCartConfirmation";
	   }
	 
	   
	   @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.GET)
	   public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
	      CartInfo cartInfo = Utils.getCartInSession(request);
	 
	      if (cartInfo == null || cartInfo.isEmpty()) {
	 
	         return "redirect:/shoppingCart";
	      } else if (!cartInfo.isValidCustomer()) {
	 
	         return "redirect:/shoppingCartCustomer";
	      }
	      model.addAttribute("myCart", cartInfo);
	 
	      return "shoppingCartConfirmation";
	   }
	 
	   
	   @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.POST)
	   public String shoppingCartConfirmationSave(HttpServletRequest request, Model model) {
	   	CartInfo cartInfo = Utils.getCartInSession(request);

	   	if (cartInfo.isEmpty()) {
	   		return "redirect:/shoppingCart";
	   	}
	   	if (!cartInfo.isValidCustomer()) {
	   		return "redirect:/shoppingCartCustomer";
	   	}

	   	try {
	   		orderDAO.saveOrder(cartInfo);
	   	} catch (Exception e) {
	   		return "shoppingCartConfirmation";
	   	}

	   	CustomerInfo customerInfo = cartInfo.getCustomerInfo();

	   	StringBuilder content = new StringBuilder();
	   	content.append(String.format("Dear %s,\n\n", customerInfo.getName()));
	   	content.append(String.format("Your order #%d was successfully created!\n", cartInfo.getOrderNum()));
	   	content.append("Order items:\n");
	   	for(CartLineInfo info: cartInfo.getCartLines()) {
	   		content.append(String.format(" * %s [%d] - $%f\n", info.getProductInfo().getName(), info.getQuantity(), info.getAmount()));
	   	}
	   	content.append(String.format("Total price: $%f\n", cartInfo.getAmountTotal()));
	   	content.append("Order info:\n");
	   	content.append(String.format("  Name: %s\n", customerInfo.getName()));
	   	content.append(String.format("  Phone: %s\n", customerInfo.getPhone()));
	   	content.append(String.format("  Address: %s\n", customerInfo.getAddress()));
	   	content.append("For further instructions our assistance will contact you soon\n");
	   	content.append("\nThanks for using our services!\n");
//	   	content.append("{shop-name}");

	   	Mail mail = new Mail();
	   	mail.setTo(customerInfo.getEmail());
	   	mail.setSubject(String.format("Order #%d", cartInfo.getOrderNum()));
	   	mail.setContent(content.toString());

	   	emailService.sendMessage(mail);

	   	Utils.removeCartInSession(request);
	   	Utils.storeLastOrderedCartInSession(request, cartInfo);

	   	return "redirect:/shoppingCartFinalize";
	   }
	   @RequestMapping(value = { "/shoppingCartFinalize" }, method = RequestMethod.GET)
	   public String shoppingCartFinalize(HttpServletRequest request, Model model) {
	 
	      CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);
	 
	      if (lastOrderedCart == null) {
	         return "redirect:/shoppingCart";
	      }
	      model.addAttribute("lastOrderedCart", lastOrderedCart);
	      return "shoppingCartFinalize";
	   }
	 
	   @RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
	   public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
	         @RequestParam("code") String code) throws IOException {
	      Product product = null;
	      if (code != null) {
	         product = this.productDAO.findProduct(code);
	      }
	      if (product != null && product.getImage() != null) {
	         response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
	         response.getOutputStream().write(product.getImage());
	      }
	      response.getOutputStream().close();
	   }

}
