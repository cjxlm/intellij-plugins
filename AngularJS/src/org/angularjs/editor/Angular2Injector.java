package org.angularjs.editor;

import com.intellij.lang.css.CSSLanguage;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.impl.JSLiteralExpressionImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.util.ObjectUtils;
import org.angularjs.codeInsight.attributes.AngularAttributesRegistry;
import org.angularjs.index.AngularIndexUtil;
import org.angularjs.index.AngularJS2IndexingHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class Angular2Injector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    final Project project = context.getProject();
    if (!AngularIndexUtil.hasAngularJS2(project)) return;

    final PsiElement parent = context.getParent();
    if (context instanceof JSLiteralExpressionImpl && ((JSLiteralExpressionImpl)context).isQuotedLiteral()) {
      if (!(parent instanceof JSArrayLiteralExpression)) return;

      final JSProperty property = ObjectUtils.tryCast(parent.getParent(), JSProperty.class);
      if (property != null && "styles".equals(property.getName())) {
        final JSCallExpression callExpression = PsiTreeUtil.getParentOfType(property, JSCallExpression.class);
        final JSExpression expression = callExpression != null ? callExpression.getMethodExpression() : null;
        if (expression instanceof JSReferenceExpression) {
          final String name = ((JSReferenceExpression)expression).getReferenceName();
          if (!AngularJS2IndexingHandler.isDirective(name)) return;

          final TextRange range = ElementManipulators.getValueTextRange(context);
          registrar.startInjecting(CSSLanguage.INSTANCE).
            addPlace(null, null, (PsiLanguageInjectionHost)context, range).
            doneInjecting();
        }
      }
      return;
    }
    if (context instanceof XmlAttributeValueImpl && parent instanceof XmlAttribute) {
      final int length = context.getTextLength();
      if ((AngularAttributesRegistry.isEventAttribute(((XmlAttribute)parent).getName(), project) ||
           AngularAttributesRegistry.isBindingAttribute(((XmlAttribute)parent).getName(), project)) &&
          length > 0) {
        registrar.startInjecting(JavascriptLanguage.INSTANCE).
          addPlace(null, null, (PsiLanguageInjectionHost)context, ElementManipulators.getValueTextRange(context)).
          doneInjecting();
      }
    }
  }

  @NotNull
  @Override
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(JSLiteralExpressionImpl.class, XmlAttributeValueImpl.class);
  }
}
